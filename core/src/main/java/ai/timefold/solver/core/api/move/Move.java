package ai.timefold.solver.core.api.move;

import java.util.Collection;

import ai.timefold.solver.core.impl.heuristic.move.AbstractSimplifiedMove;

/**
 * The base interface against which to implement moves.
 *
 * <p>
 * Moves are expected to be immutable.
 * (Think records, later value records with Valhalla.)
 * This means they cannot have any mutable state.
 * However, sometimes it will be necessary for a move to compute some state based on its inputs.
 * (For example: if a list change move knows the value to move and the value to move it behind,
 * it needs to compute the entities in whose lists those values sit,
 * and the positions at which they sit. This is the role of the context.)
 * The solver will control the context and if necessary, after the move is undone, can recompute the context
 * to provide the latest state to the toString() methods etc.
 *
 * <p>
 * Simple moves which do not require context can be written against {@link ContextlessMove}.
 *
 * @param <Solution_>
 * @param <Context_>
 */
public interface Move<Solution_, Context_> {

    /**
     * Called by the solver before a move is run.
     * The result of this call will be passed to all the other methods in the move,
     * when the solver calls them.
     *
     * @param solutionState never null; allows to read values of the variables.
     * @return may be null, with {@link ContextlessMove}.
     */
    Context_ prepareContext(SolutionState<Solution_> solutionState);

    /**
     * The equivalent to doMove() from the old API.
     * There is no undo move.
     *
     * @param mutableSolutionState Exposes all possible mutative operations on the variables.
     *        Remembers those mutative operations and can replay them in reverse order
     *        when the solver needs to undo the move.
     *        We already have this functionality in the old API via the {@link AbstractSimplifiedMove}.
     * @param context
     */
    void run(MutableSolutionState<Solution_> mutableSolutionState, Context_ context);

    /**
     * Equivalent of the rebase() method from the old API.
     * Used in multi-threaded solving.
     *
     * @param solutionState never null
     * @param context
     * @return The rebased move.
     */
    Move<Solution_, Context_> rebase(SolutionState<Solution_> solutionState, Context_ context);

    /**
     * Equivalent of getPlanningEntities() from the old API.
     * Used in tabu search.
     *
     * @param context
     * @return Entities that should become tabu.
     */
    Collection<?> getPlanningEntities(Context_ context);

    /**
     * Equivalent of getPlanningValues() from the old API.
     * Used in tabu search.
     *
     * @param context
     * @return Values that should become tabu.
     */
    Collection<?> getPlanningValues(Context_ context);

    /**
     * The solver will make sure to only call this when the move is actually printed out during debug logging.
     * This will eliminate all overhead of toString on the hot path, incl. having to store information for later undo.
     *
     * @param context
     * @return A description of the move.
     */
    String toString(Context_ context);

}
