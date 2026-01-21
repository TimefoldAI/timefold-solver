package ai.timefold.solver.core.preview.api.move;

import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for executing moves on a bound planning solution instance.
 * <p>
 * Created via {@link MoveRunner#using(Object)}, this context binds a specific solution
 * instance to the runner and exposes execution methods.
 * <p>
 * This class is NOT thread-safe.
 * <p>
 * <strong>This type is part of the Preview API which is under development.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 * Migration support will be provided via OpenRewrite recipes when breaking changes occur.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveRunContext<Solution_> {

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
     * @throws IllegalStateException if the parent MoveRunner has been closed
     */
    void execute(Move<Solution_> move);

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
     * @throws IllegalStateException if the parent MoveRunner has been closed
     * @throws Error if the move throws an Error (Errors are never suppressed)
     */
    void execute(Move<Solution_> move, Consumer<Exception> exceptionHandler);

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
     * @throws IllegalStateException if the parent MoveRunner has been closed
     */
    void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> assertions);

}
