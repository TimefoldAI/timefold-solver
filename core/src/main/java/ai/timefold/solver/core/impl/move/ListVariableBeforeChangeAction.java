package ai.timefold.solver.core.impl.move;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * Records what a list variable range looked like before a change, so undo can restore it.
 * <p>
 * {@link VariableChangeRecordingScoreDirector} records this on {@code beforeListVariableChanged} and
 * completes it with {@link #setToIndex(int)} on the matching {@code afterListVariableChanged},
 * so the pair becomes a single undo step:
 * undo fires one {@code afterListVariableChanged} notification instead of two,
 * the second of which would only redo the shadow-variable re-indexing the first one already did,
 * since nothing reads it in between.
 * <p>
 * The recorder rejects every event sequence that would leave an action unmerged,
 * so {@link #undo(VariableDescriptorAwareScoreDirector)} only ever runs on a merged action.
 * It restores exactly the range the bracket declared;
 * a change the move made <i>outside</i> that range is not recorded here
 * and is therefore lost on undo.
 * That is the move's own contract to keep -
 * nothing on this path can see it, and it surfaces only indirectly,
 * through the undo-corruption diagnosis of {@link EnvironmentMode#TRACKED_FULL_ASSERT}.
 */
final class ListVariableBeforeChangeAction<Solution_, Entity_, Value_> implements ChangeAction<Solution_> {

    private final Entity_ entity;
    private final List<Value_> oldValue;
    private final int fromIndex;
    private final int toIndex;
    private final int originalListSize;
    private final ListVariableDescriptor<Solution_> variableDescriptor;

    /**
     * The {@code toIndex} of the matching {@code afterListVariableChanged} call -
     * the current, post-mutation end of the range that undo must clear before restoring {@link #oldValue} -
     * or {@code -1} while the bracket is still open.
     */
    private int mutatedToIndex = -1;

    ListVariableBeforeChangeAction(Entity_ entity, List<Value_> oldValue, int fromIndex, int toIndex,
            int originalListSize, ListVariableDescriptor<Solution_> variableDescriptor) {
        this.entity = entity;
        this.oldValue = oldValue;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.originalListSize = originalListSize;
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
     * The size the entity's list variable had when this bracket opened,
     * so the recorder can verify that the matching {@code afterListVariableChanged}
     * reports the length the mutation actually produced.
     */
    int originalListSize() {
        return originalListSize;
    }

    /**
     * Called by {@link VariableChangeRecordingScoreDirector#afterListVariableChanged}
     * once that call has been validated against this bracket.
     *
     * @param toIndex the {@code toIndex} of that {@code afterListVariableChanged} call
     */
    void setToIndex(int toIndex) {
        this.mutatedToIndex = toIndex;
    }

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        if (mutatedToIndex < 0) {
            throw new IllegalStateException(
                    "Impossible state: the beforeListVariableChanged (%d, %d) of entity (%s) was never closed by its afterListVariableChanged."
                            .formatted(fromIndex, toIndex, entity));
        }
        // before() over the current (mutated) range, after() over the restored range.
        scoreDirector.beforeListVariableChanged(variableDescriptor, entity, fromIndex, mutatedToIndex);
        var list = variableDescriptor.getValue(entity);
        list.subList(fromIndex, mutatedToIndex).clear();
        list.addAll(fromIndex, oldValue);
        scoreDirector.afterListVariableChanged(variableDescriptor, entity, fromIndex, toIndex);
    }

    @Override
    public ChangeAction<Solution_> rebase(Lookup lookup) {
        var rebasedValueList = oldValue.stream().map(lookup::lookUpWorkingObject).toList();
        var rebased = new ListVariableBeforeChangeAction<>(lookup.lookUpWorkingObject(entity), rebasedValueList,
                fromIndex, toIndex, originalListSize, variableDescriptor);
        // Otherwise the rebased copy would undo as if its bracket were still open.
        rebased.mutatedToIndex = mutatedToIndex;
        return rebased;
    }

}
