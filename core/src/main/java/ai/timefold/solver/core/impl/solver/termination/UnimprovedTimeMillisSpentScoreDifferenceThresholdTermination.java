package ai.timefold.solver.core.impl.solver.termination;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Queue;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;
import ai.timefold.solver.core.impl.util.Pair;

public final class UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<Solution_>
        extends AbstractTermination<Solution_> {

    private final long unimprovedTimeMillisSpentLimit;
    private final Score unimprovedScoreDifferenceThreshold;
    private final Clock clock;

    private Queue<Pair<Long, Score>> bestScoreImprovementHistoryQueue;
    // safeTimeMillis is until when we're safe from termination
    private long solverSafeTimeMillis = -1L;
    private long phaseSafeTimeMillis = -1L;
    private boolean currentPhaseSendsBestSolutionEvents = false;

    public UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination(long unimprovedTimeMillisSpentLimit,
            Score<?> unimprovedScoreDifferenceThreshold) {
        this(unimprovedTimeMillisSpentLimit, unimprovedScoreDifferenceThreshold, Clock.systemUTC());
    }

    UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination(long unimprovedTimeMillisSpentLimit,
            Score<?> unimprovedScoreDifferenceThreshold, Clock clock) {
        this.unimprovedTimeMillisSpentLimit = unimprovedTimeMillisSpentLimit;
        if (unimprovedTimeMillisSpentLimit < 0L) {
            throw new IllegalArgumentException("The unimprovedTimeMillisSpentLimit (%d) cannot be negative."
                    .formatted(unimprovedTimeMillisSpentLimit));
        }
        this.unimprovedScoreDifferenceThreshold = unimprovedScoreDifferenceThreshold;
        this.clock = clock;
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        resetState();
    }

    void resetState() {
        bestScoreImprovementHistoryQueue = new ArrayDeque<>();
        solverSafeTimeMillis = clock.millis() + unimprovedTimeMillisSpentLimit;
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        bestScoreImprovementHistoryQueue = null;
        solverSafeTimeMillis = -1L;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseSafeTimeMillis = phaseScope.getStartingSystemTimeMillis() + unimprovedTimeMillisSpentLimit;
        /*
         * Construction heuristics and similar phases only trigger best solution events at the end.
         * This means that these phases only provide a meaningful result at their end.
         * Unimproved time spent termination is not useful for these phases,
         * as it would terminate the solver prematurely,
         * skipping any useful phases that follow it, such as local search.
         * We avoid that by never terminating during these phases,
         * and resetting the counter to zero when the next phase starts.
         */
        currentPhaseSendsBestSolutionEvents = phaseScope.isPhaseSendingBestSolutionEvents();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        phaseSafeTimeMillis = -1L;
        if (!currentPhaseSendsBestSolutionEvents) { // The next phase starts all over.
            resetState();
        }
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        if (stepScope.getBestScoreImproved()) {
            var solverScope = stepScope.getPhaseScope().getSolverScope();
            long bestSolutionTimeMillis = solverScope.getBestSolutionTimeMillis();
            var bestScore = solverScope.getBestScore();
            for (var it = bestScoreImprovementHistoryQueue.iterator(); it.hasNext();) {
                var bestScoreImprovement = it.next();
                var scoreDifference = bestScore.subtract(bestScoreImprovement.value());
                var timeLimitNotYetReached = bestScoreImprovement.key()
                        + unimprovedTimeMillisSpentLimit >= bestSolutionTimeMillis;
                var scoreImprovedOverThreshold = scoreDifference.compareTo(unimprovedScoreDifferenceThreshold) >= 0;
                if (scoreImprovedOverThreshold && timeLimitNotYetReached) {
                    it.remove();
                    var safeTimeMillis = bestSolutionTimeMillis + unimprovedTimeMillisSpentLimit;
                    solverSafeTimeMillis = safeTimeMillis;
                    phaseSafeTimeMillis = safeTimeMillis;
                } else {
                    break;
                }
            }
            bestScoreImprovementHistoryQueue.add(new Pair<>(bestSolutionTimeMillis, bestScore));
        }
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(solverSafeTimeMillis);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(phaseSafeTimeMillis);
    }

    private boolean isTerminated(long safeTimeMillis) {
        if (!currentPhaseSendsBestSolutionEvents) { // This phase never terminates early.
            return false;
        }
        // It's possible that there is already an improving move in the forager
        // that will end up pushing the safeTimeMillis further
        // but that doesn't change the fact that the best score didn't improve enough in the specified time interval.
        // It just looks weird because it terminates even though the final step is a high enough score improvement.
        var now = clock.millis();
        return now > safeTimeMillis;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return calculateTimeGradient(solverSafeTimeMillis);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateTimeGradient(phaseSafeTimeMillis);
    }

    private double calculateTimeGradient(long safeTimeMillis) {
        if (!currentPhaseSendsBestSolutionEvents) {
            return 0.0;
        }
        var now = clock.millis();
        var unimprovedTimeMillisSpent = now - (safeTimeMillis - unimprovedTimeMillisSpentLimit);
        var timeGradient = unimprovedTimeMillisSpent / ((double) unimprovedTimeMillisSpentLimit);
        return Math.min(timeGradient, 1.0);
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<Solution_> createChildThreadTermination(
            SolverScope<Solution_> solverScope, ChildThreadType childThreadType) {
        return new UnimprovedTimeMillisSpentScoreDifferenceThresholdTermination<>(unimprovedTimeMillisSpentLimit,
                unimprovedScoreDifferenceThreshold);
    }

    @Override
    public String toString() {
        return "UnimprovedTimeMillisSpent(" + unimprovedTimeMillisSpentLimit + ")";
    }
}
