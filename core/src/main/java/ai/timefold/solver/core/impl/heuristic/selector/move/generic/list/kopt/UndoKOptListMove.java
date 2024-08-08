package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.AbstractUndoMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * A K-Opt move that does the list rotation before performing the flips instead of after, allowing
 * it to act as the undo move of a K-Opt move that does the rotation after the flips.
 *
 * @param <Solution_>
 */
public final class UndoKOptListMove<Solution_> extends AbstractUndoMove<Solution_> {
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final List<FlipSublistAction> equivalent2Opts;
    private final int preShiftAmount;
    private final int[] newEndIndices;

    private final Object[] originalEntities;

    UndoKOptListMove(KOptListMove<Solution_> parentMove,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            List<FlipSublistAction> equivalent2Opts,
            int preShiftAmount,
            int[] newEndIndices,
            Object[] originalEntities) {
        super(parentMove);
        this.listVariableDescriptor = listVariableDescriptor;
        this.equivalent2Opts = equivalent2Opts;
        this.preShiftAmount = preShiftAmount;
        this.newEndIndices = newEndIndices;
        this.originalEntities = originalEntities;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;

        var combinedList = KOptListMove.computeCombinedList(listVariableDescriptor, originalEntities);
        combinedList.actOnAffectedElements(
                listVariableDescriptor,
                originalEntities,
                (entity, start, end) -> innerScoreDirector.beforeListVariableChanged(listVariableDescriptor, entity,
                        start,
                        end));

        // subLists will get corrupted by ConcurrentModifications, so do the operations
        // on a clone
        var combinedListCopy = combinedList.copy();
        Collections.rotate(combinedListCopy, preShiftAmount);
        combinedListCopy.moveElementsOfDelegates(newEndIndices);

        for (var move : equivalent2Opts) {
            move.doMoveOnGenuineVariables(combinedListCopy);
        }
        combinedList.applyChangesFromCopy(combinedListCopy);
        combinedList.actOnAffectedElements(listVariableDescriptor,
                originalEntities,
                (entity, start, end) -> innerScoreDirector.afterListVariableChanged(listVariableDescriptor, entity,
                        start,
                        end));
    }

}
