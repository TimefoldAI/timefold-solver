package ai.timefold.solver.core.impl.heuristic.move;

import java.util.List;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class RecordedUndoMove<Solution_> extends AbstractUndoMove<Solution_> {

    private final List<ChangeAction<Solution_>> changeActions;

    RecordedUndoMove(AbstractSimplifiedMove<Solution_> simplifiedMove, List<ChangeAction<Solution_>> changeActions) {
        super(simplifiedMove);
        this.changeActions = changeActions;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        for (int i = changeActions.size() - 1; i >= 0; i--) { // Iterate in reverse.
            var changeAction = changeActions.get(i);
            changeAction.undo(innerScoreDirector);
        }
        scoreDirector.triggerVariableListeners();
    }

}