package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class UnimprovedMoveCountTermination<Solution_> extends AbstractTermination<Solution_> {

    private final long unimprovedMoveCountLimit;

    public UnimprovedMoveCountTermination(long unimprovedMoveCountLimit) {
        this.unimprovedMoveCountLimit = unimprovedMoveCountLimit;
        if (unimprovedMoveCountLimit < 0) {
            throw new IllegalArgumentException("The unimprovedMoveCountLimit (%d) cannot be negative."
                    .formatted(unimprovedMoveCountLimit));
        }
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        var unimprovedMoveCount = calculateUnimprovedMoveCount(phaseScope);
        return unimprovedMoveCount >= unimprovedMoveCountLimit;
    }

    private static long calculateUnimprovedMoveCount(AbstractPhaseScope<?> phaseScope) {
        var bestSolutionMoveCount = phaseScope.getBestSolutionMoveCalculationCount();
        var lastStepMoveCalculationCount = phaseScope.getLastCompletedStepScope().getMoveCalculationCount();
        return lastStepMoveCalculationCount - bestSolutionMoveCount;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        throw new UnsupportedOperationException(
                "%s can only be used for phase termination.".formatted(getClass().getSimpleName()));
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        var unimprovedMoveCount = calculateUnimprovedMoveCount(phaseScope);
        var timeGradient = unimprovedMoveCount / ((double) unimprovedMoveCountLimit);
        return Math.min(timeGradient, 1.0);
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public UnimprovedMoveCountTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new UnimprovedMoveCountTermination<>(unimprovedMoveCountLimit);
    }

    @Override
    public String toString() {
        return "unimprovedMoveCountTermination(%d)".formatted(unimprovedMoveCountLimit);
    }

}
