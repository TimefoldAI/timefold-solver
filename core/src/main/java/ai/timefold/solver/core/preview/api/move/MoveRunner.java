package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.impl.move.DefaultMoveRunner;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for executing {@link Move}s on planning solutions.
 * <p>
 * Provides a fluent API for testing move implementations in both permanent and temporary modes.
 * Designed for testing and development use cases, not production solving workflows.
 * <p>
 * This class is NOT thread-safe. Each thread must create its own MoveRunner instance.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 *     var solutionMetaModel = PlanningSolutionMetaModel.of(MySolution.class, MyEntity.class);
 *     var runner = MoveRunner.build(solutionMetaModel);
 *     var basicVariable = solutionMetaModel.genuineEntity(MyEntity.class)
 *          .basicVariable();
 *
 *     var move = Moves.change(basicVariable, ..., ...);
 *
 *     // Permanent execution
 *     var context = runner.using(solution);
 *     context.execute(move);
 *
 *     // Temporary execution with automatic undo
 *     context.executeTemporarily(move, view -> {
 *         assertThat(view.getValue(...)).isEqualTo(expected);
 *     });
 * }</pre>
 * <p>
 * <strong>This type is part of the Preview API which is under development.</strong>
 * There are no guarantees for backward compatibility; any class, method, or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 * Migration support will be provided via OpenRewrite recipes when breaking changes occur.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface MoveRunner<Solution_> {

    /**
     * Creates a new MoveRunner for the given solution and entity classes.
     * <p>
     * This method validates inputs, and initializes many internal structures.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     *
     * @param solutionMetaModel the planning solution class;
     *        use {@link PlanningSolutionMetaModel#of(Class, Class[])} to build one.
     * @param <Solution_> the planning solution type
     * @return a new MoveRunner instance
     */
    static <Solution_> MoveRunner<Solution_> build(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultMoveRunner<>(solutionMetaModel);
    }

    /**
     * Creates an execution context for the given solution instance.
     * <p>
     * This method creates a score director from the cached factory and sets the working solution,
     * which automatically triggers shadow variable initialization for the provided solution.
     * <p>
     * Multiple execution contexts can be created from the same MoveRunner instance,
     * allowing sequential move execution with different solutions or the same solution
     * at different points in time.
     *
     * @param solution the planning solution instance
     * @return a new execution context bound to the given solution with initialized shadow variables
     */
    MoveRunContext<Solution_> using(Solution_ solution);

}
