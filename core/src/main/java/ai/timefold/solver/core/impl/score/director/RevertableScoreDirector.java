package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RevertableScoreDirector<Solution_> extends VariableDescriptorAwareScoreDirector<Solution_> {

    /**
     * Use this method to get a representation of the operation that will be performed by {@link #undoChanges()}.
     * This operation will keep accumulating uncommitted changes until {@link #undoChanges()} is actually called.
     * After that happens, any subsequent call to this method will result in a fresh move instance with only those
     * operations that happened after the latest call to {@link #undoChanges()}.
     * This is useful when the undo operation ever needs to be replayed manually; most use cases do not need this
     * and should refer to {@link MoveDirector#executeTemporary(Move)}.
     */
    Move<Solution_> createUndoMove();

    /**
     * Use this method to revert all changes made by moves.
     * The score director that implements this logic must be able to track every single change in the solution and
     * restore it to its original state.
     */
    void undoChanges();
}
