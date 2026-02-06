package ai.timefold.solver.core.preview.api.move;

import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for executing moves on a bound planning solution instance.
 * <p>
 * Created via {@link MoveTester#using(Object)}, this context binds a specific solution
 * instance to the runner and exposes execution methods.
 * <p>
 * This class is NOT thread-safe.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility; any method or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveTestContext<Solution_> {

    /**
     * Executes the given move permanently on the bound solution.
     * <p>
     * Changes made by the move persist after this method returns.
     * Shadow variables are automatically updated via the solver's existing mechanisms.
     * <p>
     * If the move throws an exception, the exception propagates to the caller
     * and the solution state may be only partially modified.
     * In this case, the caller must discard the solution instance
     * and get a new instance of this class via {@link MoveTester#using(Object)}.
     *
     * @param move the move to execute
     */
    void execute(Move<Solution_> move);

    /**
     * Executes the given move temporarily on the bound solution, runs assertions,
     * then automatically undoes the move.
     * <p>
     * The move is executed, modifying the solution state.
     * The assertions callback is then invoked, allowing the caller to verify the modified state.
     * Finally, the move is automatically undone, restoring the solution to its exact pre-execution state.
     * <p>
     * <strong>Important constraints:</strong>
     * <ul>
     * <li>Nesting executeTemporarily() calls is not supported and results in undefined behavior</li>
     * <li>Do not modify the solution state directly within the assertions callback; doing so
     * results in undefined behavior with unpredictable undo results</li>
     * <li>If an exception occurs during move execution, the callback, or undo operation,
     * the solution state is undefined and no restoration is attempted.
     * The caller must discard the solution instance
     * and get a new instance of this class via {@link MoveTester#using(Object)}.</li>
     * </ul>
     *
     * @param move the move to execute temporarily
     * @param callback to verify the modified solution state before it is undone;
     *        receives a {@link SolutionView} for read-only access
     */
    void executeTemporarily(Move<Solution_> move, Consumer<SolutionView<Solution_>> callback);

}
