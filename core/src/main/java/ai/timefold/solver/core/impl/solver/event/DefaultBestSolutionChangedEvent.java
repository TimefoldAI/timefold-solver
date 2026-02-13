package ai.timefold.solver.core.impl.solver.event;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBestSolutionChangedEvent<Solution_> implements BestSolutionChangedEvent<Solution_> {

    private final Solver<Solution_> solver;
    private final EventProducerId producerId;
    private final long timeMillisSpent;
    private final Solution_ newBestSolution;
    private final Score newBestScore;
    private final boolean isNewBestSolutionInitialized;
    private final int unassignedCount;

    public DefaultBestSolutionChangedEvent(Solver<Solution_> solver, EventProducerId eventProducerId, long timeMillisSpent,
            Solution_ newBestSolution, InnerScore newBestScore) {
        this.solver = solver;
        this.producerId = eventProducerId;
        this.timeMillisSpent = timeMillisSpent;
        this.newBestSolution = newBestSolution;
        this.newBestScore = newBestScore.raw();
        this.isNewBestSolutionInitialized = newBestScore.isFullyAssigned();
        this.unassignedCount = newBestScore.unassignedCount();
    }

    public int getUnassignedCount() {
        return unassignedCount;
    }

    @Override
    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    @Override
    public EventProducerId getProducerId() {
        return producerId;
    }

    @Override
    public Solution_ getNewBestSolution() {
        return newBestSolution;
    }

    @Override
    public Score getNewBestScore() {
        return newBestScore;
    }

    @Override
    public boolean isNewBestSolutionInitialized() {
        return isNewBestSolutionInitialized;
    }

    @Override
    public boolean isEveryProblemChangeProcessed() {
        return solver.isEveryProblemChangeProcessed();
    }

}
