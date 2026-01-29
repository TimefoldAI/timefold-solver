package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.impl.neighborhood.DefaultNeighborhoodEvaluator;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

/**
 * Entry point for evaluating {@link MoveProvider}s on a given solution.
 * Given a planning solution, it produces an {@link NeighborhoodEvaluationContext}
 * which contains all moves that can be generated from that solution
 * using the provided {@link MoveProvider}.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 *     var mySolution = ...;
 *     var neighborhood = NeighborhoodEvaluator.build(new MyMoveProvider(), MySolution.class, MyEntity.class)
 *          .evaluate(solution);
 *     var moveIterator = neighborhood.getMoveIterator();
 *
 *     while (moveIterator.hasNext()) {
 *         var move = moveIterator.next();
 *         // Run assertions on the move here.
 *     }
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
public interface NeighborhoodEvaluator<Solution_> {

    /**
     * Creates a new {@link NeighborhoodEvaluator} for the given move provider
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
     * @return a new {@link NeighborhoodEvaluator} instance
     */
    static <Solution_> NeighborhoodEvaluator<Solution_> build(MoveProvider<Solution_> moveProvider,
            PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        return new DefaultNeighborhoodEvaluator<>(moveProvider, solutionMetaModel);
    }

    /**
     * Creates an evaluation context for the given solution instance.
     * Once you have the context,
     * you can retrieve the moves via methods such as {@link NeighborhoodEvaluationContext#getMovesAsStream()}.
     * <p>
     * Different evaluation contexts can be created, each bound to a different solution instance.
     * They will operate independently of each other.
     *
     * @param solution the planning solution instance
     * @return a new execution context bound to the given solution
     */
    NeighborhoodEvaluationContext<Solution_> using(Solution_ solution);

}
