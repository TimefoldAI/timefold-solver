package ai.timefold.solver.core.api.move.generic;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;

/**
 * Makes no changes.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class NoChangeMove<Solution_> implements Move<Solution_> {

    public static final NoChangeMove<?> INSTANCE = new NoChangeMove<>();

    public static <Solution_> NoChangeMove<Solution_> getInstance() {
        return (NoChangeMove<Solution_>) INSTANCE;
    }

    private NoChangeMove() {
        // No external instances allowed.
    }

    @Override
    public void run(MutableSolutionState<Solution_> mutableSolutionState) {
        // Do nothing.
    }

    @SuppressWarnings("unchecked")
    @Override
    public Move<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return (Move<Solution_>) INSTANCE;
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public Collection<?> getPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "No change";
    }
}
