package ai.timefold.solver.core.impl.solver.event;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NonNull;

public final class DefaultBestSolutionChangedEvent<Solution_> extends BestSolutionChangedEvent<Solution_> {
    public static String SOLVING_STARTED_EVENT_ID = "Solving started";
    public static String PROBLEM_CHANGE_EVENT_ID = "Problem change";

    private final int unassignedCount;

    public DefaultBestSolutionChangedEvent(@NonNull Solver<Solution_> solver, EventProducerId eventProducerId,
            long timeMillisSpent,
            @NonNull Solution_ newBestSolution, @NonNull InnerScore newBestScore) {
        super(solver, eventProducerId, timeMillisSpent, newBestSolution, newBestScore.raw(), newBestScore.isFullyAssigned());
        this.unassignedCount = newBestScore.unassignedCount();
    }

    public int getUnassignedCount() {
        return unassignedCount;
    }

}
