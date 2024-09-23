package ai.timefold.solver.core.api.move;

import java.util.Collection;

/**
 * A simplified version of {@link Move} which requires no context.
 * The user will implement {@link #run(MutableSolutionState)} instead of {@link #run(MutableSolutionState, Void)}.
 *
 * @param <Solution_>
 */
public interface ContextlessMove<Solution_> extends Move<Solution_, Void> {

    @Override
    default Void prepareContext(SolutionState<Solution_> solutionState) {
        return null;
    }

    @Override
    default void run(MutableSolutionState<Solution_> mutableSolutionState, Void unused) {
        run(mutableSolutionState);
    }

    void run(MutableSolutionState<Solution_> mutableSolutionState);

    @Override
    default ContextlessMove<Solution_> rebase(SolutionState<Solution_> rebaser, Void unused) {
        return rebase(rebaser);
    }

    ContextlessMove<Solution_> rebase(SolutionState<Solution_> solutionState);

    @Override
    default Collection<?> getPlanningEntities(Void unused) {
        return getPlanningEntities();
    }

    Collection<?> getPlanningEntities();

    @Override
    default Collection<?> getPlanningValues(Void unused) {
        return getPlanningValues();
    }

    Collection<?> getPlanningValues();

    @Override
    default String toString(Void unused) {
        return toString();
    }

}
