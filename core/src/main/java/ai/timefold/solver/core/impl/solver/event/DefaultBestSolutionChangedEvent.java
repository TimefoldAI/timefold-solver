package ai.timefold.solver.core.impl.solver.event;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NonNull;

public final class DefaultBestSolutionChangedEvent<Solution_> extends BestSolutionChangedEvent<Solution_> {

    private final int unassignedCount;

    public DefaultBestSolutionChangedEvent(@NonNull Solver<Solution_> solver, long timeMillisSpent,
            @NonNull Solution_ newBestSolution, @NonNull InnerScore newBestScore) {
        super(solver, timeMillisSpent, newBestSolution, newBestScore.raw(), newBestScore.isFullyAssigned());
        this.unassignedCount = newBestScore.unassignedCount();
    }

    public int getUnassignedCount() {
        return unassignedCount;
    }

}
