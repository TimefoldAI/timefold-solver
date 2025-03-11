package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

/**
 * The only move director that supports undoing moves.
 * Moves are undone when the director is {@link #close() closed}.
 * 
 * @param <Solution_>
 */
@NullMarked
public final class EphemeralMoveDirector<Solution_> extends MoveDirector<Solution_>
        implements AutoCloseable {

    EphemeralMoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        // Doesn't require the index cache, because we maintain the invariant in this class.
        super(new VariableChangeRecordingScoreDirector<>(scoreDirector, false));
    }

    public Move<Solution_> createUndoMove() {
        return new RecordedUndoMove<>(getVariableChangeRecordingScoreDirector().copyChanges());
    }

    @Override
    public <Entity_, Value_> ElementLocation
            getPositionOf(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value) {
        return getPositionOf(getVariableChangeRecordingScoreDirector().getBacking(), variableMetaModel, value);

    }

    public VariableChangeRecordingScoreDirector<Solution_> getVariableChangeRecordingScoreDirector() {
        return (VariableChangeRecordingScoreDirector<Solution_>) scoreDirector;
    }

    @Override
    public EphemeralMoveDirector<Solution_> ephemeral() {
        throw new IllegalStateException("Impossible state: move director is already ephemeral.");
    }

    @Override
    public void close() {
        getVariableChangeRecordingScoreDirector().undoChanges();
    }

}
