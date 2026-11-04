package ai.timefold.solver.core.impl.move;

/**
 * Thrown when executing a move leaves the working solution structurally flawed,
 * for example by making shadow variables depend on each other in a cycle.
 * Its own type lets callers of a temporary move tell the flaw apart from other failures:
 * thrown while the move is still applied, the flaw is gone once the move is undone.
 */
public final class StructurallyFlawedMoveException extends IllegalArgumentException {
    public StructurallyFlawedMoveException(String message) {
        super(message);
    }
}
