package ai.timefold.solver.core.impl.move;

import java.util.function.Function;

import ai.timefold.solver.core.preview.api.move.Move;

/**
 * Thrown when executing a move leaves the working solution structurally flawed,
 * for example by making shadow variables depend on each other in a cycle.
 * It is thrown while the move is still applied.
 * <p>
 * Only {@link MoveDirector#executeTemporaryUndoingOnFailure(Move, Function)} undoes the move before rethrowing it,
 * so only its callers may treat the flaw as gone and keep using the working solution;
 * its own type lets them tell the flaw apart from other failures.
 * On every other path, such as a permanent move or the other temporary execution methods,
 * the move remains applied and the caller must treat the working solution as structurally flawed.
 */
public final class StructurallyFlawedMoveException extends IllegalArgumentException {
    public StructurallyFlawedMoveException(String message) {
        super(message);
    }
}
