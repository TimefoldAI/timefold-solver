package ai.timefold.solver.core.impl.solver.termination;

import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ScoreCalculationCountTermination<Solution_>
        extends AbstractUniversalTermination<Solution_>
        implements ChildThreadSupportingTermination<Solution_, SolverScope<Solution_>> {

    private final long scoreCalculationCountLimit;

    public ScoreCalculationCountTermination(long scoreCalculationCountLimit) {
        this.scoreCalculationCountLimit = scoreCalculationCountLimit;
        if (scoreCalculationCountLimit < 0L) {
            throw new IllegalArgumentException(
                    "The scoreCalculationCountLimit (%d) cannot be negative."
                            .formatted(scoreCalculationCountLimit));
        }
    }

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(solverScope.getScoreDirector());
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(phaseScope.getScoreDirector());
    }

    private boolean isTerminated(InnerScoreDirector<Solution_, ?> scoreDirector) {
        var scoreCalculationCount = scoreDirector.getCalculationCount();
        return scoreCalculationCount >= scoreCalculationCountLimit;
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return calculateTimeGradient(solverScope.getScoreDirector());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateTimeGradient(phaseScope.getScoreDirector());
    }

    private double calculateTimeGradient(InnerScoreDirector<Solution_, ?> scoreDirector) {
        var scoreCalculationCount = scoreDirector.getCalculationCount();
        var timeGradient = scoreCalculationCount / ((double) scoreCalculationCountLimit);
        return Math.min(timeGradient, 1.0);
    }

    @Override
    public ScoreCalculationCountTermination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        if (childThreadType == ChildThreadType.PART_THREAD) {
            // The ScoreDirector.calculationCount of partitions is maxed, not summed.
            return new ScoreCalculationCountTermination<>(scoreCalculationCountLimit);
        } else {
            throw new UnsupportedOperationException("The childThreadType (%s) is not implemented."
                    .formatted(childThreadType));
        }
    }

    @Override
    public String toString() {
        return "ScoreCalculationCount(" + scoreCalculationCountLimit + ")";
    }

}
