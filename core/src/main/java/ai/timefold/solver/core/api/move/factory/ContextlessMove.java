package ai.timefold.solver.core.api.move.factory;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.metamodel.MutableSolutionState;
import ai.timefold.solver.core.api.domain.metamodel.SolutionState;

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
    default ContextlessMove<Solution_> rebase(Rebaser rebaser, Void unused) {
        return rebase(rebaser);
    }

    ContextlessMove<Solution_> rebase(Rebaser rebaser);

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
