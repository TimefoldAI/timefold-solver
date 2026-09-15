package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * The only move director that supports undoing moves.
 * Moves are undone when the director is {@link #close() closed}.
 * This class deliberately is not {@link AutoCloseable}:
 * a try-with-resources statement would undo even a move that threw an exception,
 * whose recorded actions may be half-applied,
 * causing all sorts of unexpected situations.
 * Callers therefore close explicitly, on the paths where the move itself succeeded;
 * a move that throws terminates the execution instead,
 * never getting to the point of triggering its undo.
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
        return getVariableChangeRecordingScoreDirector().createUndoMove();
    }

    @Override
    public <Entity_, Value_> ElementPosition
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(getVariableChangeRecordingScoreDirector().getBacking(), variableMetaModel, value);

    }

    public VariableChangeRecordingScoreDirector<Solution_, Score_> getVariableChangeRecordingScoreDirector() {
        return (VariableChangeRecordingScoreDirector<Solution_, Score_>) externalScoreDirector;
    }

    public void close() {
        getVariableChangeRecordingScoreDirector().undoChanges();
    }

}
