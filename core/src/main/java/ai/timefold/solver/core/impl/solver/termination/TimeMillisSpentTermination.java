package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class TimeMillisSpentTermination<Solution_> extends AbstractTermination<Solution_> {

    private final long timeMillisSpentLimit;

    public TimeMillisSpentTermination(long timeMillisSpentLimit) {
        this.timeMillisSpentLimit = timeMillisSpentLimit;
        if (timeMillisSpentLimit < 0L) {
            throw new IllegalArgumentException("The timeMillisSpentLimit (%d) cannot be negative."
                    .formatted(timeMillisSpentLimit));
        }
    }

    public long getTimeMillisSpentLimit() {
        return timeMillisSpentLimit;
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        var solverTimeMillisSpent = solverScope.calculateTimeMillisSpentUpToNow();
        return isTerminated(solverTimeMillisSpent);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        var phaseTimeMillisSpent = phaseScope.calculatePhaseTimeMillisSpentUpToNow();
        return isTerminated(phaseTimeMillisSpent);
    }

    private boolean isTerminated(long timeMillisSpent) {
        return timeMillisSpent >= timeMillisSpentLimit;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        var solverTimeMillisSpent = solverScope.calculateTimeMillisSpentUpToNow();
        return calculateTimeGradient(solverTimeMillisSpent);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var phaseTimeMillisSpent = phaseScope.calculatePhaseTimeMillisSpentUpToNow();
        return calculateTimeGradient(phaseTimeMillisSpent);
    }

    private double calculateTimeGradient(long timeMillisSpent) {
        var timeGradient = timeMillisSpent / ((double) timeMillisSpentLimit);
        return Math.min(timeGradient, 1.0);
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public TimeMillisSpentTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new TimeMillisSpentTermination<>(timeMillisSpentLimit);
    }

    @Override
    public String toString() {
        return "TimeMillisSpent(" + timeMillisSpentLimit + ")";
    }

}
