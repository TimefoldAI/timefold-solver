package ai.timefold.solver.core.impl.solver.termination;

import java.time.Clock;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class UnimprovedTimeMillisSpentTermination<Solution_> extends AbstractTermination<Solution_> {

    private final long unimprovedTimeMillisSpentLimit;
    private final Clock clock;

    private boolean currentPhaseSendsBestSolutionEvents = false;
    private long phaseStartedTimeMillis = -1L;

    public UnimprovedTimeMillisSpentTermination(long unimprovedTimeMillisSpentLimit) {
        this(unimprovedTimeMillisSpentLimit, Clock.systemUTC());
    }

    UnimprovedTimeMillisSpentTermination(long unimprovedTimeMillisSpentLimit, Clock clock) {
        this.unimprovedTimeMillisSpentLimit = unimprovedTimeMillisSpentLimit;
        if (unimprovedTimeMillisSpentLimit < 0L) {
            throw new IllegalArgumentException("The unimprovedTimeMillisSpentLimit (%d) cannot be negative."
                    .formatted(unimprovedTimeMillisSpentLimit));
        }
        this.clock = clock;
    }

    public long getUnimprovedTimeMillisSpentLimit() {
        return unimprovedTimeMillisSpentLimit;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
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
        phaseStartedTimeMillis = clock.millis();
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        long bestSolutionTimeMillis = solverScope.getBestSolutionTimeMillis();
        return isTerminated(bestSolutionTimeMillis);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        var bestSolutionTimeMillis = phaseScope.getPhaseBestSolutionTimeMillis();
        return isTerminated(bestSolutionTimeMillis);
    }

    private boolean isTerminated(long bestSolutionTimeMillis) {
        if (!currentPhaseSendsBestSolutionEvents) { // This phase never terminates early.
            return false;
        }
        return getUnimprovedTimeMillisSpent(bestSolutionTimeMillis) >= unimprovedTimeMillisSpentLimit;
    }

    private long getUnimprovedTimeMillisSpent(long bestSolutionTimeMillis) {
        var now = clock.millis();
        return now - Math.max(bestSolutionTimeMillis, phaseStartedTimeMillis);
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        long bestSolutionTimeMillis = solverScope.getBestSolutionTimeMillis();
        return calculateTimeGradient(bestSolutionTimeMillis);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var bestSolutionTimeMillis = phaseScope.getPhaseBestSolutionTimeMillis();
        return calculateTimeGradient(bestSolutionTimeMillis);
    }

    private double calculateTimeGradient(long bestSolutionTimeMillis) {
        if (!currentPhaseSendsBestSolutionEvents) {
            return 0.0;
        }
        var timeGradient = getUnimprovedTimeMillisSpent(bestSolutionTimeMillis) / ((double) unimprovedTimeMillisSpentLimit);
        return Math.min(timeGradient, 1.0);
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public UnimprovedTimeMillisSpentTermination<Solution_> createChildThreadTermination(
            SolverScope<Solution_> solverScope, ChildThreadType childThreadType) {
        return new UnimprovedTimeMillisSpentTermination<>(unimprovedTimeMillisSpentLimit);
    }

    @Override
    public String toString() {
        return "UnimprovedTimeMillisSpent(" + unimprovedTimeMillisSpentLimit + ")";
    }
}
