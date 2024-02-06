package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;

public abstract class AbstractEasyMove<Solution_> implements Move<Solution_> {

    @Override
    public final Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        var recordingScoreDirector = new VariableChangeRecordingScoreDirector<>(scoreDirector);
        doMoveOnly(recordingScoreDirector);
        return new EasyUndoMove<>(recordingScoreDirector.getVariableChanges(), this::toString);
    }

    public final void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        doMoveOnGenuineVariables(scoreDirector);
        scoreDirector.triggerVariableListeners();
    }

    protected abstract void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector);

    @Override
    public String toString() {
        return getSimpleMoveTypeDescription();
    }

}