package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.impl.move.DefaultMoveTester;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for executing {@link Move}s on planning solutions.
 * <p>
 * Provides a fluent API for testing move implementations in both permanent and temporary modes.
 * Designed for testing and development use cases, not production solving workflows.
 * <p>
 * This class is NOT thread-safe. Each thread must create its own {@link MoveTester} instance.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 *     var solutionMetaModel = PlanningSolutionMetaModel.of(MySolution.class, MyEntity.class);
 *     var tester = MoveTester.build(solutionMetaModel);
 *     var basicVariable = solutionMetaModel.genuineEntity(MyEntity.class)
 *          .basicVariable();
 *
 *     var move = Moves.change(basicVariable, ..., ...);
 *
 *     // Permanent execution
 *     var context = tester.using(solution);
 *     context.execute(move);
 *
 *     // Temporary execution with automatic undo
 *     context.executeTemporarily(move, view -> {
 *         assertThat(view.getValue(...)).isEqualTo(expected);
 *     });
 * }</pre>
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility; any method or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveTester<Solution_> {

    /**
     * Creates a new {@link MoveTester} for the given solution and entity classes.
     * <p>
     * This method validates inputs, and initializes many internal structures.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     *
     * @param solutionMetaModel the planning solution class;
     *        use {@link PlanningSolutionMetaModel#of(Class, Class[])} to build one.
     * @param <Solution_> the planning solution type
     * @return a new instance
     */
    static <Solution_> MoveTester<Solution_> build(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultMoveTester<>(solutionMetaModel);
    }

    /**
     * Creates an execution context for the given solution instance.
     * <p>
     * This method creates a score director from the cached factory and sets the working solution,
     * which automatically triggers shadow variable initialization for the provided solution.
     * <p>
     * Multiple execution contexts can be created from the same {@link MoveTester} instance,
     * allowing sequential move execution with different solutions or the same solution
     * at different points in time.
     *
     * @param solution the planning solution instance
     * @return a new execution context bound to the given solution with initialized shadow variables
     */
    MoveTestContext<Solution_> using(Solution_ solution);

}
