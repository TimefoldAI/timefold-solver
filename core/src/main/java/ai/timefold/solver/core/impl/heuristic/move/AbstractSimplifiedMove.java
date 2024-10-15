package ai.timefold.solver.core.impl.heuristic.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.director.VariableChangeRecordingScoreDirector;

/**
 * This is an alternative to {@link AbstractMove},
 * allowing to trade some performance for less boilerplate.
 * This move will record all events that change variables,
 * and replay them in the undo move,
 * therefore removing the need to implement the undo move.
 *
 * @param <Solution_>
 */
public abstract class AbstractSimplifiedMove<Solution_> implements Move<Solution_> {

    @Override
    public final Move<Solution_> doMove(ScoreDirector<Solution_> scoreDirector) {
        var recordingScoreDirector =
                scoreDirector instanceof VariableChangeRecordingScoreDirector<Solution_> variableChangeRecordingScoreDirector
                        ? variableChangeRecordingScoreDirector
                        : new VariableChangeRecordingScoreDirector<>(scoreDirector);
        doMoveOnly(recordingScoreDirector);
        return new RecordedUndoMove<>(this, recordingScoreDirector::undoChanges);
    }

    @Override
    public final void doMoveOnly(ScoreDirector<Solution_> scoreDirector) {
        // LegacyMoveAdapter does not wrap the score director in a VariableChangeRecordingScoreDirector
        var recordingScoreDirector =
                scoreDirector instanceof VariableChangeRecordingScoreDirector<Solution_> variableChangeRecordingScoreDirector
                        ? variableChangeRecordingScoreDirector
                        : new VariableChangeRecordingScoreDirector<>(scoreDirector);
        doMoveOnGenuineVariables(recordingScoreDirector);
        recordingScoreDirector.triggerVariableListeners();
    }

    protected abstract void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector);

    @Override
    public String toString() {
        return getSimpleMoveTypeDescription();
    }

}