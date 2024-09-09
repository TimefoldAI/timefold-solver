package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class MoveCountTermination<Solution_> extends AbstractTermination<Solution_> {

    private final long moveCountLimit;

    public MoveCountTermination(long moveCountLimit) {
        this.moveCountLimit = moveCountLimit;
        if (moveCountLimit < 0L) {
            throw new IllegalArgumentException("The moveCountLimit (%d) cannot be negative.".formatted(moveCountLimit));
        }
    }

    public long getMoveCountLimit() {
        return moveCountLimit;
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(solverScope);
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(phaseScope.getSolverScope());
    }

    private boolean isTerminated(SolverScope<Solution_> solverScope) {
        long moveEvaluationCount = solverScope.getMoveEvaluationCount();
        return moveEvaluationCount >= moveCountLimit;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return calculateTimeGradient(solverScope);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateTimeGradient(phaseScope.getSolverScope());
    }

    private double calculateTimeGradient(SolverScope<Solution_> solverScope) {
        var moveEvaluationCount = solverScope.getMoveEvaluationCount();
        var timeGradient = moveEvaluationCount / ((double) moveCountLimit);
        return Math.min(timeGradient, 1.0);
    }
    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public MoveCountTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        return new MoveCountTermination<>(moveCountLimit);
    }

    @Override
    public String toString() {
        return "MoveCount(%d)".formatted(moveCountLimit);
    }

}
