package ai.timefold.solver.core.preview.api.move;

import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for executing moves on a bound planning solution instance.
 * <p>
 * Created via {@link MoveRunner#using(Object)}, this context binds a specific solution
 * instance to the runner and exposes execution methods.
 * <p>
 * This class is NOT thread-safe.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public final class MoveRunContext<Solution_> {

    private final MoveRunner<Solution_> moveRunner;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final Solution_ solution;

    MoveRunContext(MoveRunner<Solution_> moveRunner,
            InnerScoreDirector<Solution_, ?> scoreDirector,
            Solution_ solution) {
        this.moveRunner = moveRunner;
        this.scoreDirector = scoreDirector;
        this.solution = solution;
    }

    /**
     * Executes the given move permanently on the bound solution.
     * <p>
     * Changes made by the move persist after this method returns.
     * Shadow variables are automatically updated via the solver's existing mechanisms.
     * <p>
     * If the move throws an exception, the exception propagates to the caller
     * and the solution state may be partially modified.
     *
     * @param move the move to execute; must not be null
     * @throws NullPointerException if move is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     */
    public void execute(Move<Solution_> move) {
        Objects.requireNonNull(move, "move");
        scoreDirector.executeMove(move);
    }

    /**
     * Executes the given move permanently on the bound solution with exception handling.
     * <p>
     * If the move throws an {@link Exception} (not an {@link Error}), the exception handler
     * is invoked and exception propagation is suppressed (caller continues normally).
     * {@link Error}s always propagate and are never suppressed.
     * <p>
     * No automatic rollback occurs on exception. The solution state may be partially modified
     * when an exception occurs. The exception handler allows the caller to control failure handling.
     *
     * @param move the move to execute; must not be null
     * @param exceptionHandler handles exceptions thrown during move execution; must not be null;
     *        invoked only for {@link Exception} subclasses, not {@link Error}s
     * @throws NullPointerException if move or exceptionHandler is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     * @throws Error if the move throws an Error (Errors are never suppressed)
     */
    public void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler) {
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(exceptionHandler, "exceptionHandler");

        try {
            scoreDirector.executeMove(move);
        } catch (Error e) {
            // Errors always propagate
            throw e;
        } catch (Exception e) {
            // Exception: invoke handler and suppress propagation
            exceptionHandler.accept(e);
            // If it's an InterruptedException, restore the interrupt status
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Executes the given move temporarily on the bound solution, runs assertions,
     * then automatically undoes the move.
     * <p>
     * The move is executed, modifying the solution state. The assertions callback is then invoked,
     * allowing the caller to verify the modified state. Finally, the move is automatically undone,
     * restoring the solution to its exact pre-execution state.
     * <p>
     * <strong>Important constraints:</strong>
     * <ul>
     * <li>Nesting executeTemporarily() calls is not supported and results in undefined behavior</li>
     * <li>Do not modify the solution state directly within the assertions callback; doing so
     * results in undefined behavior with unpredictable undo results</li>
     * <li>If an exception occurs during move execution, assertions callback, or undo operation,
     * the solution state is UNDEFINED and no restoration is attempted. The caller must
     * discard the solution instance.</li>
     * </ul>
     *
     * @param move the move to execute temporarily; must not be null
     * @param assertions callback to verify the modified solution state; receives a {@link SolutionView}
     *        for read-only access; must not be null
     * @throws NullPointerException if move or assertions is null
     * @throws IllegalStateException if the parent MoveRunner has been closed
     */
    public void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> assertions) {
        Objects.requireNonNull(move, "move");
        Objects.requireNonNull(assertions, "assertions");
        scoreDirector.executeTemporarily(move, assertions);
    }
}
