package ai.timefold.solver.core.impl.move.director;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.RevertableScoreDirector;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorCache;

public final class VariableChangeRecordingScoreDirector<Solution_, Score_ extends Score<Score_>>
        implements RevertableScoreDirector<Solution_> {

    private final InnerScoreDirector<Solution_, Score_> backingScoreDirector;
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

    public VariableChangeRecordingScoreDirector(ScoreDirector<Solution_> backingScoreDirector) {
        this(backingScoreDirector, true);
    }

    public VariableChangeRecordingScoreDirector(ScoreDirector<Solution_> backingScoreDirector, boolean requiresIndexCache) {
        this.backingScoreDirector = (InnerScoreDirector<Solution_, Score_>) backingScoreDirector;
        this.cache = requiresIndexCache ? new IdentityHashMap<>() : null;
        // Intentional LinkedList; fast clear, no allocations upfront,
        // will most often only carry a small number of items.
        this.variableChanges = new LinkedList<>();
    }

    private VariableChangeRecordingScoreDirector(InnerScoreDirector<Solution_, Score_> backingScoreDirector,
            List<ChangeAction<Solution_>> variableChanges, Map<Object, Integer> cache) {
        this.backingScoreDirector = backingScoreDirector;
        this.variableChanges = variableChanges;
        this.cache = cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChangeAction<Solution_>> copyChanges() {
        return List.copyOf(variableChanges);
    }

    @Override
    public void undoChanges() {
        var changeCount = variableChanges.size();
        if (changeCount == 0) {
            return;
        }
        var listIterator = variableChanges.listIterator(changeCount);
        while (listIterator.hasPrevious()) { // Iterate in reverse.
            var changeAction = listIterator.previous();
            changeAction.undo(backingScoreDirector);
        }
        Objects.requireNonNull(backingScoreDirector).triggerVariableListeners();
        variableChanges.clear();
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        variableChanges.add(new VariableChangeAction<>(entity, variableDescriptor.getValue(entity), variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeVariableChanged(variableDescriptor, entity);
        }
    }

    @Override
    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        if (backingScoreDirector != null) {
            backingScoreDirector.afterVariableChanged(variableDescriptor, entity);
        }
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
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        }
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
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        }
    }

    @Override
    public void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableBeforeAssignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableElementAssigned(variableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableAfterAssignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableElementAssigned(variableDescriptor, element);
        }
    }

    @Override
    public void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableBeforeUnassignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        variableChanges.add(new ListVariableAfterUnassignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
        }
    }

    // For other operations, call the delegate's method.

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return Objects.requireNonNull(backingScoreDirector).getSolutionDescriptor();
    }

    /**
     * Returns the score director to which events are delegated.
     */
    public InnerScoreDirector<Solution_, Score_> getBacking() {
        return backingScoreDirector;
    }

    /**
     * The {@code VariableChangeRecordingScoreDirector} score director includes two main tasks:
     * tracking any variable change and firing events to a delegated score director.
     * This method returns a copy of the score director
     * that only tracks variable changes without firing any delegated score director events.
     */
    public VariableChangeRecordingScoreDirector<Solution_, Score_> getNonDelegating() {
        return new VariableChangeRecordingScoreDirector<>(null, variableChanges, cache);
    }

    @Override
    public Solution_ getWorkingSolution() {
        return Objects.requireNonNull(backingScoreDirector).getWorkingSolution();
    }

    @Override
    public VariableDescriptorCache<Solution_> getVariableDescriptorCache() {
        return Objects.requireNonNull(backingScoreDirector).getVariableDescriptorCache();
    }

    @Override
    public void triggerVariableListeners() {
        variableChanges.add(new TriggerVariableListenersAction<>());
        if (backingScoreDirector != null) {
            backingScoreDirector.triggerVariableListeners();
        }
    }

    @Override
    public <E> E lookUpWorkingObject(E externalObject) {
        return Objects.requireNonNull(backingScoreDirector).lookUpWorkingObject(externalObject);
    }

    @Override
    public <E> E lookUpWorkingObjectOrReturnNull(E externalObject) {
        return Objects.requireNonNull(backingScoreDirector).lookUpWorkingObjectOrReturnNull(externalObject);
    }

}
