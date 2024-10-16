package ai.timefold.solver.core.impl.move.director;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorCache;

public final class VariableChangeRecordingScoreDirector<Solution_> implements VariableDescriptorAwareScoreDirector<Solution_> {

    private final InnerScoreDirector<Solution_, ?> delegate;
    private final List<ChangeAction<Solution_>> variableChanges;
    /*
     * The fromIndex of afterListVariableChanged must match the fromIndex of its beforeListVariableChanged call.
     * Otherwise this will happen in the undo move:
     *
     * // beforeListVariableChanged(0, 3);
     * [1, 2, 3, 4]
     * change
     * [1, 2, 3]
     * // afterListVariableChanged(2, 3)
     * // Start Undo
     * // Undo afterListVariableChanged(2, 3)
     * [1, 2, 3] -> [1, 2]
     * // Undo beforeListVariableChanged(0, 3);
     * [1, 2, 3, 4, 1, 2]
     *
     * This map exists to ensure that this is the case.
     */
    private final Map<Object, Integer> cache;

    public VariableChangeRecordingScoreDirector(ScoreDirector<Solution_> delegate) {
        this(delegate, true);
    }

    public VariableChangeRecordingScoreDirector(ScoreDirector<Solution_> delegate, boolean requiresIndexCache) {
        this.delegate = (InnerScoreDirector<Solution_, ?>) delegate;
        this.cache = requiresIndexCache ? new IdentityHashMap<>() : null;
        // Intentional LinkedList; fast clear, no allocations upfront,
        // will most often only carry a small number of items.
        this.variableChanges = new LinkedList<>();
    }

    List<ChangeAction<Solution_>> copyVariableChanges() {
        return List.copyOf(variableChanges);
    }

    public void undoChanges() {
        var changeCount = variableChanges.size();
        if (changeCount == 0) {
            return;
        }
        var listIterator = variableChanges.listIterator(changeCount);
        while (listIterator.hasPrevious()) { // Iterate in reverse.
            var changeAction = listIterator.previous();
            changeAction.undo(delegate);
        }
        delegate.triggerVariableListeners();
        variableChanges.clear();
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        variableChanges.add(new VariableChangeAction<>(entity, variableDescriptor.getValue(entity), variableDescriptor));
        delegate.beforeVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        delegate.afterVariableChanged(variableDescriptor, entity);
    }

    @Override
    public void beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        // List is fromIndex, fromIndex, since the undo action for afterListVariableChange will clear the affected list
        if (cache != null) {
            cache.put(entity, fromIndex);
        }
        var list = variableDescriptor.getValue(entity);
        variableChanges.add(new ListVariableBeforeChangeAction<>(entity,
                List.copyOf(list.subList(fromIndex, toIndex)), fromIndex, toIndex,
                variableDescriptor));
        delegate.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        if (cache != null) {
            Integer requiredFromIndex = cache.remove(entity);
            if (requiredFromIndex != fromIndex) {
                throw new IllegalArgumentException(
                        """
                                The fromIndex of afterListVariableChanged (%d) must match the fromIndex of its beforeListVariableChanged counterpart (%d).
                                Maybe check implementation of your %s."""
                                .formatted(fromIndex, requiredFromIndex, AbstractMove.class.getSimpleName()));
            }
        }
        variableChanges.add(new ListVariableAfterChangeAction<>(entity, fromIndex, toIndex, variableDescriptor));
        delegate.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableBeforeAssignmentAction<>(element, variableDescriptor));
        delegate.beforeListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableAfterAssignmentAction<>(element, variableDescriptor));
        delegate.afterListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableBeforeUnassignmentAction<>(element, variableDescriptor));
        delegate.beforeListVariableElementUnassigned(variableDescriptor, element);
    }

    @Override
    public void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableAfterUnassignmentAction<>(element, variableDescriptor));
        delegate.afterListVariableElementUnassigned(variableDescriptor, element);
    }

    // For other operations, call the delegate's method.

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return delegate.getSolutionDescriptor();
    }

    public InnerScoreDirector<Solution_, ?> getDelegate() {
        return delegate;
    }

    @Override
    public Solution_ getWorkingSolution() {
        return delegate.getWorkingSolution();
    }

    @Override
    public VariableDescriptorCache<Solution_> getVariableDescriptorCache() {
        return delegate.getVariableDescriptorCache();
    }

    @Override
    public void triggerVariableListeners() {
        delegate.triggerVariableListeners();
    }

    @Override
    public <E> E lookUpWorkingObject(E externalObject) {
        return delegate.lookUpWorkingObject(externalObject);
    }

    @Override
    public <E> E lookUpWorkingObjectOrReturnNull(E externalObject) {
        return delegate.lookUpWorkingObjectOrReturnNull(externalObject);
    }

    @Override
    public void changeVariableFacade(VariableDescriptor<Solution_> variableDescriptor, Object entity, Object newValue) {
        beforeVariableChanged(variableDescriptor, entity);
        variableDescriptor.setValue(entity, newValue);
        afterVariableChanged(variableDescriptor, entity);
    }

    /**
     * Record a list assignment.
     * Used by moves with nested phases, such as ruin and recreate.
     * These moves need to record some changes manually,
     * because they cannot influence what will happen in the nested phases.
     * Cannot be sent via the before/after events, because we don't want to delegate to the actual score director;
     * these new changes should only be used for undoing the move(s).
     *
     * @param variableDescriptor never null
     * @param entity never null
     * @param values never null, may be empty
     */
    public void recordListAssignment(ListVariableDescriptor<Solution_> variableDescriptor, Object entity,
            List<Object> values) {
        for (var element : values) {
            variableChanges.add(new ListVariableBeforeAssignmentAction<>(element, variableDescriptor));
        }
        variableChanges.add(new ListVariableAfterChangeAction<>(entity,
                variableDescriptor.getFirstUnpinnedIndex(entity), variableDescriptor.getListSize(entity),
                variableDescriptor));
        for (var element : values) {
            variableChanges.add(new ListVariableAfterAssignmentAction<>(element, variableDescriptor));
        }
    }
}
