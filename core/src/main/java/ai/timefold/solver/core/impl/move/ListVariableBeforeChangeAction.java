package ai.timefold.solver.core.impl.move;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * Records what a list variable range looked like before a change, so undo can restore it.
 * <p>
 * Normally paired with a {@link ListVariableAfterChangeAction} recorded right after the change is
 * applied. {@link VariableChangeRecordingScoreDirector} merges the two into this single action via
 * {@link #merge(int)}, instead of keeping them as two separate undo steps, so undo fires one
 * {@code afterListVariableChanged} notification instead of two - the second one would only redo
 * the shadow-variable re-indexing the first one already did, since nothing reads it in between.
 * <p>
 * A pairing is not always observed on this same recording instance - see
 * {@link ListVariableAfterChangeAction} - in which case this action stays unmerged and undoes on
 * its own, exactly as before this class supported merging at all.
 */
final class ListVariableBeforeChangeAction<Solution_, Entity_, Value_> implements ChangeAction<Solution_> {

    private final Entity_ entity;
    private final List<Value_> oldValue;
    private final int fromIndex;
    private final int toIndex;
    private final ListVariableDescriptor<Solution_> variableDescriptor;

    private boolean merged = false;
    private int mutatedToIndex;

    ListVariableBeforeChangeAction(Entity_ entity, List<Value_> oldValue, int fromIndex, int toIndex,
            ListVariableDescriptor<Solution_> variableDescriptor) {
        this.entity = entity;
        this.oldValue = oldValue;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.variableDescriptor = variableDescriptor;
    }

    Entity_ entity() {
        return entity;
    }

    List<Value_> oldValue() {
        return oldValue;
    }

    int fromIndex() {
        return fromIndex;
    }

    int toIndex() {
        return toIndex;
    }

    /**
     * Called by {@link VariableChangeRecordingScoreDirector#afterListVariableChanged}
     * when the matching {@code after} call for this same bracket is observed,
     * instead of appending a separate {@link ListVariableAfterChangeAction}.
     *
     * @param mutatedToIndex the {@code toIndex} of that {@code afterListVariableChanged} call -
     *        the current, post-mutation end of the range that undo must clear before restoring {@link #oldValue}
     */
    void merge(int mutatedToIndex) {
        this.merged = true;
        this.mutatedToIndex = mutatedToIndex;
    }

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        if (merged) {
            // before() over the current (mutated) range, after() over the restored range.
            scoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, mutatedToIndex);
            @SuppressWarnings("unchecked")
            var mutatedItems = (List<Value_>) variableDescriptor.getValue(entity).subList(fromIndex, mutatedToIndex);
            mutatedItems.clear();
            variableDescriptor.getValue(entity).addAll(fromIndex, oldValue);
            scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
            return;
        }
        if (oldValue.isEmpty()) {
            // Nothing was captured here (fromIndex == toIndex), so there is nothing to restore.
            // The sibling ListVariableAfterChangeAction's undo already fired
            // the equivalent (fromIndex, fromIndex) notification for this same range;
            // avoid firing it again.
            return;
        }
        variableDescriptor.getValue(entity).addAll(fromIndex, oldValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public ChangeAction<Solution_> rebase(Lookup lookup) {
        var rebasedValueList = oldValue.stream().map(lookup::lookUpWorkingObject).toList();
        var rebased = new ListVariableBeforeChangeAction<>(lookup.lookUpWorkingObject(entity), rebasedValueList,
                fromIndex, toIndex, variableDescriptor);
        if (merged) {
            // Otherwise the rebased copy would silently fall through to the unmerged undo path
            // and corrupt the list instead of restoring it.
            rebased.merge(mutatedToIndex);
        }
        return rebased;
    }

}
