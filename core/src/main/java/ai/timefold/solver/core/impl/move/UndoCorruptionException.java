package ai.timefold.solver.core.impl.move;

/**
 * Thrown when undoing a temporarily executed move fails,
 * leaving the working solution in an unknown state.
 */
public final class UndoCorruptionException extends IllegalStateException {
    public UndoCorruptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
