package ai.timefold.solver.core.api.move;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.heuristic.move.AbstractSimplifiedMove;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.MoveTabuAcceptor;

/**
 * The base interface against which to implement moves.
 *
 * <p>
 * A Move represents a change of 1 or more {@link PlanningVariable}s of 1 or more {@link PlanningEntity}s
 * in the working {@link PlanningSolution}.
 * <p>
 * Usually the move holds a direct reference to each {@link PlanningEntity} of the {@link PlanningSolution}
 * which it will change when {@link #run(MutableSolutionState)} is called.
 * <p>
 * A Move should implement {@link Object#equals(Object)} and {@link Object#hashCode()} for {@link MoveTabuAcceptor}.
 * It is highly recommended to override {@link #getPlanningEntities()} and {@link #getPlanningValues()},
 * otherwise the resulting move will throw an exception when used with Tabu search.
 *
 * @param <Solution_>
 */
public interface Move<Solution_> {

    /**
     * The equivalent to doMove() from the old API.
     * There is no undo move.
     *
     * @param mutableSolutionState Exposes all possible mutative operations on the variables.
     *        Remembers those mutative operations and can replay them in reverse order
     *        when the solver needs to undo the move.
     *        We already have this functionality in the old API via the {@link AbstractSimplifiedMove}.
     */
    void run(MutableSolutionState<Solution_> mutableSolutionState);

    /**
     * Generally not required.
     * Move Streams are expected to only generate doable moves.
     * Exists to support the old Move Selector API.
     *
     * @param solutionState never null; exposes all possible read operations on the variables.
     */
    default boolean isMoveDoable(SolutionState<Solution_> solutionState) {
        return true;
    }

    /**
     * Equivalent of the rebase() method from the old API.
     * Used in multi-threaded solving.
     *
     * @param solutionState never null
     * @return The rebased move.
     */
    Move<Solution_> rebase(SolutionState<Solution_> solutionState);

    /**
     * Equivalent of getPlanningEntities() from the old API.
     * Used in tabu search.
     *
     * @return Entities that should become tabu.
     */
    default Collection<?> getPlanningEntities() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * Equivalent of getPlanningValues() from the old API.
     * Used in tabu search.
     *
     * @return Values that should become tabu.
     */
    default Collection<?> getPlanningValues() {
        throw new UnsupportedOperationException("The move (" + this + ") does not support tabu search.");
    }

    /**
     * The solver will make sure to only call this when the move is actually printed out during debug logging.
     * This will eliminate all overhead of toString on the hot path, incl. having to store information for later undo.
     *
     * @return A description of the move.
     */
    String toString();

}
