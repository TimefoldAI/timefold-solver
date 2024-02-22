package ai.timefold.solver.core.impl.heuristic.move;

import java.util.List;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class RecordedUndoMove<Solution_> implements Move<Solution_> {

    private final List<ChangeAction<Solution_>> changeActions;
    private final Supplier<String> sourceToString;

    RecordedUndoMove(List<ChangeAction<Solution_>> changeActions, Supplier<String> sourceToString) {
        this.changeActions = changeActions;
        this.sourceToString = sourceToString;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        return true;
    }

    @Override
    public Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        doMoveOnly(scoreDirector);
        return null;
    }

    @Override
    public void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        var innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        for (int i = changeActions.size() - 1; i >= 0; i--) { // Iterate in reverse.
            var changeAction = changeActions.get(i);
            changeAction.undo(innerScoreDirector);
        }
        scoreDirector.triggerVariableListeners();
    }

    @Override
    public String getSimpleMoveTypeDescription() {
        return "Undo(" + sourceToString.get() + ")";
    }

    @Override
    public String toString() {
        return getSimpleMoveTypeDescription();
    }
}