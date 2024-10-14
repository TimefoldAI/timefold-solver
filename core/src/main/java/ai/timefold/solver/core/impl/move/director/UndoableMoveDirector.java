package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;
import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * Moves are undone when the director is {@link #close() closed}.
 * 
 * @param <Solution_>
 */
public final class UndoableMoveDirector<Solution_> extends MoveDirector<Solution_>
        implements AutoCloseable {

    UndoableMoveDirector(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        // Doesn't require the index cache, because we maintain the invariant in this class.
        super(new VariableChangeRecordingScoreDirector<>(scoreDirector, false));
    }

    public Move<Solution_> createUndoMove() {
        return new RecordedUndoMove<>(getVariableChangeRecordingScoreDirector().copyVariableChanges());
    }

    @Override
    public <Entity_, Value_> ElementLocation getPositionOf(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value) {
        return getPositionOf(getVariableChangeRecordingScoreDirector().getDelegate(), variableMetaModel, value);

    }

    public VariableChangeRecordingScoreDirector<Solution_> getVariableChangeRecordingScoreDirector() {
        return (VariableChangeRecordingScoreDirector<Solution_>) scoreDirector;
    }

    @Override
    public UndoableMoveDirector<Solution_> undoable() {
        throw new IllegalStateException("Impossible state: move director is already undoable.");
    }

    @Override
    public void close() {
        getVariableChangeRecordingScoreDirector().undoChanges();
    }

}
