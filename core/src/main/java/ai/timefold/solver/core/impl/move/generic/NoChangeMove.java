package ai.timefold.solver.core.impl.move.generic;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;

import org.jspecify.annotations.NullMarked;

/**
 * Makes no changes.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public final class NoChangeMove<Solution_> implements Move<Solution_> {

    public static final NoChangeMove<?> INSTANCE = new NoChangeMove<>();

    @SuppressWarnings("unchecked")
    public static <Solution_> NoChangeMove<Solution_> getInstance() {
        return (NoChangeMove<Solution_>) INSTANCE;
    }

    private NoChangeMove() {
        // No external instances allowed.
    }

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        // Do nothing.
    }

    @SuppressWarnings("unchecked")
    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return (Move<Solution_>) INSTANCE;
    }

    @Override
    public Collection<?> extractPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public Collection<?> extractPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "No change";
    }
}
