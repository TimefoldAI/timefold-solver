package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * The only move director that supports undoing moves.
 * Moves are undone when the director is {@link #close() closed}.
 * The class can not be made {@link AutoCloseable},
 * as using it in a try-with-resources statement would mean undo would happen even on moves that threw exceptions,
 * causing all sorts of unexpected situations.
 * This way, the move throws an exception and terminates the execution,
 * therefore never even getting to the point of triggering an undo for this move.
 * 
 * @param <Solution_>
 */
@NullMarked
final class EphemeralMoveDirector<Solution_, Score_ extends Score<Score_>>
        extends MoveDirector<Solution_, Score_> {

    EphemeralMoveDirector(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        super(scoreDirector);
    }

    Move<Solution_> createUndoMove() {
        return new RecordedUndoMove<>(getVariableChangeRecordingScoreDirector().copyChanges());
    }

    @Override
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(getVariableChangeRecordingScoreDirector().getBacking(), variableMetaModel, value);

    }

    public VariableChangeRecordingScoreDirector<Solution_, Score_> getVariableChangeRecordingScoreDirector() {
        return (VariableChangeRecordingScoreDirector<Solution_, Score_>) externalScoreDirector;
    }

    @Override
    public <Result_> Result_ executeTemporary(Move<Solution_> move,
            TemporaryMovePostprocessor<Solution_, Score_, Result_> postprocessor) {
        throw new UnsupportedOperationException("Impossible state: This move director does not support undoing moves.");
    }

    public void close() {
        getVariableChangeRecordingScoreDirector().undoChanges();
    }

}
