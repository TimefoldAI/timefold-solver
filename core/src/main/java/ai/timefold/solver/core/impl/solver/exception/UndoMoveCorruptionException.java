package ai.timefold.solver.core.impl.solver.exception;

import ai.timefold.solver.core.impl.move.MoveDirector;

/**
 * Thrown when undoing a temporarily executed move fails,
 * leaving the working solution in an unknown state.
 * Unlike {@link UndoScoreCorruptionException}, which reports a wrong score after an undo,
 * this reports an undo that itself failed; see {@link MoveDirector}.
 */
public final class UndoMoveCorruptionException extends IllegalStateException {
    public UndoMoveCorruptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
