package ai.timefold.solver.core.impl.solver.termination;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BestScoreFeasibleTermination<Solution_>
        extends AbstractUniversalTermination<Solution_> {

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

    private static boolean isTerminated(InnerScore<?> innerScore) {
        return innerScore.isFullyAssigned() && innerScore.raw().isFeasible();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        return calculateFeasibilityTimeGradient(InnerScore.fullyAssigned((Score) solverScope.getStartingInitializedScore()),
                solverScope.getBestScore().raw());
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        return calculateFeasibilityTimeGradient(phaseScope.getStartingScore(), phaseScope.getBestScore().raw());
    }

    <Score_ extends Score<Score_>> double calculateFeasibilityTimeGradient(@Nullable InnerScore<Score_> innerStartScore,
            Score_ score) {
        if (innerStartScore == null || !innerStartScore.isFullyAssigned()) {
            return 0.0;
        }
        var startScore = innerStartScore.raw();
        var totalDiff = startScore.negate();
        var totalDiffNumbers = totalDiff.toLevelNumbers();
        var scoreDiff = score.subtract(startScore);
        var scoreDiffNumbers = scoreDiff.toLevelNumbers();
        if (scoreDiffNumbers.length != totalDiffNumbers.length) {
            throw new IllegalStateException("The startScore (" + startScore + ") and score (" + score
                    + ") don't have the same levelsSize.");
        }
        return BestScoreTermination.calculateTimeGradient(totalDiffNumbers, scoreDiffNumbers,
                timeGradientWeightFeasibleNumbers, feasibleLevelsSize);
    }

    @Override
    public String toString() {
        return "BestScoreFeasible()";
    }

}
