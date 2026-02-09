package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.impl.neighborhood.DefaultNeighborhoodTester;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for evaluating {@link MoveProvider}s on a given solution.
 * Given a planning solution, it produces an {@link NeighborhoodTestContext}
 * which contains all moves that can be generated from that solution
 * using the provided {@link MoveProvider}.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * var solutionMetaModel = PlanningSolutionMetaModel.of(MySolution.class, MyEntity.class);
 * var context = NeighborhoodTester.build(new MyMoveProvider(), solutionMetaModel)
 *         .using(solution);
 * var moveIterator = context.getMovesAsIterator();
 *
 * while (moveIterator.hasNext()) {
 *     var move = moveIterator.next();
 *     // Run assertions on the move here.
 * }
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
public interface NeighborhoodTester<Solution_> {

    /**
     * Creates a new {@link NeighborhoodTester} for the given move provider
     * and the given solution and entity classes.
     * <p>
     * This method validates inputs, and initializes many internal structures.
     * These are heavy operations performed once and cached for reuse.
     * <p>
     * Shadow variables are initialized later when a solution is bound via {@link #using(Object)}.
     *
     * @param moveProvider the move provider to generate moves
     * @param solutionMetaModel the planning solution meta-model;
     *        use {@link PlanningSolutionMetaModel#of(Class, Class[])} to build one.
     * @param <Solution_> the planning solution type
     * @return a new {@link NeighborhoodTester} instance
     */
    static <Solution_> NeighborhoodTester<Solution_> build(MoveProvider<Solution_> moveProvider,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultNeighborhoodTester<>(moveProvider, solutionMetaModel);
    }

    /**
     * Creates an evaluation context for the given solution instance.
     * Once you have the context,
     * you can retrieve the moves via methods such as {@link NeighborhoodTestContext#getMovesAsStream()}.
     * <p>
     * Different evaluation contexts can be created, each bound to a different solution instance.
     * They will operate independently of each other.
     *
     * @param solution the planning solution instance
     * @return a new execution context bound to the given solution
     */
    NeighborhoodTestContext<Solution_> using(Solution_ solution);

}
