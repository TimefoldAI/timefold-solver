package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSelectorBasedMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin.SelectorBasedListRuinRecreateMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.RevertableScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorCache;
import ai.timefold.solver.core.preview.api.move.Move;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NullMarked
public final class VariableChangeRecordingScoreDirector<Solution_, Score_ extends Score<Score_>>
        implements RevertableScoreDirector<Solution_> {

    private final @Nullable InnerScoreDirector<Solution_, Score_> backingScoreDirector;
    private @Nullable List<ChangeAction<Solution_>> variableChangeList;
    private boolean variableChangesEscaped = false;

    /**
     * Tracks before-actions awaiting their matching after-call so the two can be merged into one undo step,
     * and so that every such call can be validated against the bracket it claims to close
     * (see {@link #afterListVariableChanged}).
     * Shared with {@link #getNonDelegating()}'s copy, same as {@link #variableChangeList},
     * since a before/after pair can be split across the two (see {@link SelectorBasedListRuinRecreateMove}).
     */
    private final PendingListChangeTracker pendingListChangeTracker;

    public VariableChangeRecordingScoreDirector(ScoreDirector<Solution_> backingScoreDirector) {
        this.backingScoreDirector = (InnerScoreDirector<Solution_, Score_>) backingScoreDirector;
        this.pendingListChangeTracker = new PendingListChangeTracker();
    }

    private VariableChangeRecordingScoreDirector(@Nullable InnerScoreDirector<Solution_, Score_> backingScoreDirector,
                                                 List<ChangeAction<Solution_>> variableChangeList, PendingListChangeTracker pendingListChangeTracker) {
        this.backingScoreDirector = backingScoreDirector;
        this.variableChangeList = variableChangeList;
        this.pendingListChangeTracker = pendingListChangeTracker;
    }

    @Override
    public Move<Solution_> createUndoMove() {
        // The list would normally be copied here to prevent any outside modification.
        // However, copying this list on the hot path would be a major performance issue.
        // Instead, the list is passed as a reference here, and instead of it being cleared by undoChanges(),
        // the reference is replaced; that way, the move does not actually share the list with anyone,
        // and copying of its contents can be avoided.
        variableChangesEscaped = true;
        return new RecordedUndoMove<>(variableChangeList == null ? Collections.emptyList() : variableChangeList);
    }

    @Override
    public void undoChanges() {
        var changeList = getVariableChangeList();
        var changeCount = changeList.size();
        if (changeCount == 0) {
            return;
        }
        var listIterator = changeList.listIterator(changeCount);
        while (listIterator.hasPrevious()) { // Iterate in reverse.
            var changeAction = listIterator.previous();
            changeAction.undo(backingScoreDirector);
        }
        Objects.requireNonNull(backingScoreDirector).updateShadowVariables();
        resetVariableChangeList();
        pendingListChangeTracker.clear();
    }

    private List<ChangeAction<Solution_>> getVariableChangeList() {
        if (variableChangeList == null) {
            // We use an ArrayList, as LinkedList is slow to iterate and brings node allocation overhead.
            // We use a small initial capacity, as many moves will not perform that many operations.
            // The minimum is 2 - a single change, and shadow var update;
            // we use 4 to give some room for marginally more expensive operations as well,
            // without allocating the full default capacity.
            variableChangeList = new ArrayList<>(4);
        }
        return variableChangeList;
    }

    private void resetVariableChangeList() {
        if (variableChangesEscaped) {
            // We need to reallocate the list, as createUndoMove() may hold a reference to it.
            variableChangeList = null;
            variableChangesEscaped = false;
        } else {
            variableChangeList.clear(); // Do not reallocate the list on the hot path.
        }
    }

    @Override
    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        getVariableChangeList()
                .add(new VariableChangeAction<>(entity, variableDescriptor.getValue(entity), variableDescriptor));
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
        var list = variableDescriptor.getValue(entity);
        var action = new ListVariableBeforeChangeAction<>(entity,
                List.copyOf(list.subList(fromIndex, toIndex)), fromIndex, toIndex, list.size(),
                variableDescriptor);
        // Rejects a second bracket for an entity whose previous one is still open.
        pendingListChangeTracker.put(entity, action);
        getVariableChangeList().add(action);
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        }
    }

    @Override
    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
                                         int toIndex) {
        // The tracker is shared with getNonDelegating()'s copy, so a pair split across the two still matches.
        var pendingBeforeAction = pendingListChangeTracker.remove(entity);
        if (pendingBeforeAction == null) {
            throw new IllegalArgumentException("""
                    The afterListVariableChanged (%d, %d) of entity (%s) has no matching beforeListVariableChanged.
                    Maybe check implementation of your %s."""
                    .formatted(fromIndex, toIndex, entity, AbstractSelectorBasedMove.class.getSimpleName()));
        }
        var requiredFromIndex = pendingBeforeAction.fromIndex();
        if (requiredFromIndex != fromIndex) {
            /*
             * Otherwise this will happen in the undo move:
             *
             * // beforeListVariableChanged(0, 3);
             * [1, 2, 3, 4]
             * change
             * [1, 2, 3]
             * // afterListVariableChanged(2, 3)
             * // Undo restores oldValue at index 2 instead of index 0.
             */
            throw new IllegalArgumentException("""
                    The fromIndex of afterListVariableChanged (%d) must match the fromIndex of its beforeListVariableChanged counterpart (%d).
                    Maybe check implementation of your %s."""
                    .formatted(fromIndex, requiredFromIndex, AbstractSelectorBasedMove.class.getSimpleName()));
        }
        // The reported range must account for every element the mutation added or removed;
        // undo clears exactly [fromIndex, toIndex) before restoring,
        // so a range that is too short leaves elements behind
        // and one that is too long deletes elements that were never captured.
        var actualLengthDelta = variableDescriptor.getValue(entity).size() - pendingBeforeAction.originalListSize();
        var reportedLengthDelta = toIndex - pendingBeforeAction.toIndex();
        if (actualLengthDelta != reportedLengthDelta) {
            throw new IllegalArgumentException("""
                    The afterListVariableChanged (%d, %d) of entity (%s) reports a length change of (%d), \
                    but its list variable actually changed length by (%d).
                    Maybe check implementation of your %s; \
                    its beforeListVariableChanged/afterListVariableChanged range must cover everything it changed."""
                    .formatted(fromIndex, toIndex, entity, reportedLengthDelta, actualLengthDelta,
                            AbstractSelectorBasedMove.class.getSimpleName()));
        }
        // merge() mutates pendingBeforeAction in place;
        // nothing needs to happen with it afterward here,
        // because it is the SAME instance already sitting in variableChangeList
        // (added in beforeListVariableChanged(), which put it in both variableChangeList and this tracker).
        // undoChanges() will later read that mutation directly off the list.
        pendingBeforeAction.merge(toIndex);
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
        }
    }

    @Override
    public void beforeListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        getVariableChangeList().add(new ListVariableBeforeAssignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableElementAssigned(variableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementAssigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        getVariableChangeList().add(new ListVariableAfterAssignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableElementAssigned(variableDescriptor, element);
        }
    }

    @Override
    public void beforeListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        getVariableChangeList().add(new ListVariableBeforeUnassignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.beforeListVariableElementUnassigned(variableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(ListVariableDescriptor<Solution_> variableDescriptor, Object element) {
        getVariableChangeList().add(new ListVariableAfterUnassignmentAction<>(element, variableDescriptor));
        if (backingScoreDirector != null) {
            backingScoreDirector.afterListVariableElementUnassigned(variableDescriptor, element);
        }
    }

    // For other operations, call the delegate's method.

    @Override
    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return Objects.requireNonNull(backingScoreDirector).getSolutionDescriptor();
    }

    @Override
    public ValueRangeManager<Solution_> getValueRangeManager() {
        return Objects.requireNonNull(getBacking()).getValueRangeManager();
    }

    /**
     * Returns the score director to which events are delegated.
     */
    public @Nullable InnerScoreDirector<Solution_, Score_> getBacking() {
        return backingScoreDirector;
    }

    /**
     * The {@code VariableChangeRecordingScoreDirector} score director includes two main tasks:
     * tracking any variable change and firing events to a delegated score director.
     * This method returns a copy of the score director
     * that only tracks variable changes without firing any delegated score director events.
     */
    public VariableChangeRecordingScoreDirector<Solution_, Score_> getNonDelegating() {
        return new VariableChangeRecordingScoreDirector<>(null, getVariableChangeList(), pendingListChangeTracker);
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
    public void updateShadowVariables() {
        getVariableChangeList().add(UpdateShadowVariablesAction.instance());
        if (backingScoreDirector != null) {
            backingScoreDirector.updateShadowVariables();
        }
    }

    @Override
    public <E> @Nullable E lookUpWorkingObject(@Nullable E externalObject) {
        return Objects.requireNonNull(backingScoreDirector).lookUpWorkingObject(externalObject);
    }

}
