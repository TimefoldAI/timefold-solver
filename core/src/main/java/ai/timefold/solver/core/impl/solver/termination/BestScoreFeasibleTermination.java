package ai.timefold.solver.core.impl.solver.termination;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class BestScoreFeasibleTermination<Solution_> extends AbstractTermination<Solution_> {

    private final int feasibleLevelsSize;
    private final double[] timeGradientWeightFeasibleNumbers;

    public BestScoreFeasibleTermination(ScoreDefinition<?> scoreDefinition, double[] timeGradientWeightFeasibleNumbers) {
        this.feasibleLevelsSize = scoreDefinition.getFeasibleLevelsSize();
        this.timeGradientWeightFeasibleNumbers = timeGradientWeightFeasibleNumbers;
        if (timeGradientWeightFeasibleNumbers.length != this.feasibleLevelsSize - 1) {
            throw new IllegalStateException(
                    "The timeGradientWeightNumbers (%s)'s length (%d) is not 1 less than the feasibleLevelsSize (%d)."
                            .formatted(Arrays.toString(timeGradientWeightFeasibleNumbers),
                                    timeGradientWeightFeasibleNumbers.length, scoreDefinition.getFeasibleLevelsSize()));
        }
    }

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(solverScope.getBestScore());
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(phaseScope.getBestScore());
    }

    private static boolean isTerminated(Score<?> bestScore) {
        return bestScore.isFeasible();
    }

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return calculateFeasibilityTimeGradient(solverScope.getStartingInitializedScore(), solverScope.getBestScore());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateFeasibilityTimeGradient((Score) phaseScope.getStartingScore(), (Score) phaseScope.getBestScore());
    }

    <Score_ extends Score<Score_>> double calculateFeasibilityTimeGradient(Score_ startScore, Score_ score) {
        if (startScore == null || !startScore.isSolutionInitialized()) {
            return 0.0;
        }
        Score_ totalDiff = startScore.negate();
        Number[] totalDiffNumbers = totalDiff.toLevelNumbers();
        Score_ scoreDiff = score.subtract(startScore);
        Number[] scoreDiffNumbers = scoreDiff.toLevelNumbers();
        if (scoreDiffNumbers.length != totalDiffNumbers.length) {
            throw new IllegalStateException("The startScore (" + startScore + ") and score (" + score
                    + ") don't have the same levelsSize.");
        }
        return BestScoreTermination.calculateTimeGradient(totalDiffNumbers, scoreDiffNumbers,
                timeGradientWeightFeasibleNumbers, feasibleLevelsSize);
    }

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public Termination<Solution_> createChildThreadTermination(SolverScope<Solution_> solverScope,
            ChildThreadType childThreadType) {
        // TODO FIXME through some sort of solverlistener and async behaviour...
        throw new UnsupportedOperationException("This terminationClass (" + getClass()
                + ") does not yet support being used in child threads of type (" + childThreadType + ").");
    }

    @Override
    public String toString() {
        return "BestScoreFeasible()";
    }

}
