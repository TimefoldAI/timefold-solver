package ai.timefold.solver.core.impl.solver.termination;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

public final class BestScoreTermination<Solution_> extends AbstractTermination<Solution_> {

    private final int levelsSize;
    private final Score bestScoreLimit;
    private final double[] timeGradientWeightNumbers;

    public BestScoreTermination(ScoreDefinition<?> scoreDefinition, Score<?> bestScoreLimit,
            double[] timeGradientWeightNumbers) {
        levelsSize = scoreDefinition.getLevelsSize();
        this.bestScoreLimit = bestScoreLimit;
        if (bestScoreLimit == null) {
            throw new IllegalArgumentException("The bestScoreLimit cannot be null.");
        }
        this.timeGradientWeightNumbers = timeGradientWeightNumbers;
        if (timeGradientWeightNumbers.length != levelsSize - 1) {
            throw new IllegalStateException(
                    "The timeGradientWeightNumbers (%s)'s length (%d) is not 1 less than the levelsSize (%d)."
                            .formatted(Arrays.toString(timeGradientWeightNumbers), timeGradientWeightNumbers.length,
                                    scoreDefinition.getLevelsSize()));
        }
    }

    // ************************************************************************
    // Terminated methods
    // ************************************************************************

    @Override
    public boolean isSolverTerminated(SolverScope<Solution_> solverScope) {
        return isTerminated(solverScope.isBestSolutionInitialized(), solverScope.getBestScore());
    }

    @Override
    public boolean isPhaseTerminated(AbstractPhaseScope<Solution_> phaseScope) {
        return isTerminated(phaseScope.isBestSolutionInitialized(), (Score) phaseScope.getBestScore());
    }

    private boolean isTerminated(boolean bestSolutionInitialized, Score bestScore) {
        return bestSolutionInitialized && bestScore.compareTo(bestScoreLimit) >= 0;
    }

    // ************************************************************************
    // Time gradient methods
    // ************************************************************************

    @Override
    public double calculateSolverTimeGradient(SolverScope<Solution_> solverScope) {
        var startingInitializedScore = solverScope.getStartingInitializedScore();
        var bestScore = solverScope.getBestScore();
        return calculateTimeGradient(startingInitializedScore, bestScoreLimit, bestScore);
    }

    @Override
    public double calculatePhaseTimeGradient(AbstractPhaseScope<Solution_> phaseScope) {
        Score startingInitializedScore = phaseScope.getStartingScore();
        Score bestScore = phaseScope.getBestScore();
        return calculateTimeGradient(startingInitializedScore, bestScoreLimit, bestScore);
    }

    <Score_ extends Score<Score_>> double calculateTimeGradient(Score_ startScore, Score_ endScore, Score_ score) {
        var totalDiff = endScore.subtract(startScore);
        var totalDiffNumbers = totalDiff.toLevelNumbers();
        var scoreDiff = score.subtract(startScore);
        var scoreDiffNumbers = scoreDiff.toLevelNumbers();
        if (scoreDiffNumbers.length != totalDiffNumbers.length) {
            throw new IllegalStateException("The startScore (" + startScore + "), endScore (" + endScore
                    + ") and score (" + score + ") don't have the same levelsSize.");
        }
        return calculateTimeGradient(totalDiffNumbers, scoreDiffNumbers, timeGradientWeightNumbers,
                levelsSize);
    }

    /**
     *
     * @param totalDiffNumbers never null
     * @param scoreDiffNumbers never null
     * @param timeGradientWeightNumbers never null
     * @param levelDepth The number of levels of the diffNumbers that are included
     * @return {@code 0.0 <= value <= 1.0}
     */
    static double calculateTimeGradient(Number[] totalDiffNumbers, Number[] scoreDiffNumbers,
            double[] timeGradientWeightNumbers, int levelDepth) {
        var timeGradient = 0.0;
        var remainingTimeGradient = 1.0;
        for (var i = 0; i < levelDepth; i++) {
            double levelTimeGradientWeight;
            if (i != (levelDepth - 1)) {
                levelTimeGradientWeight = remainingTimeGradient * timeGradientWeightNumbers[i];
                remainingTimeGradient -= levelTimeGradientWeight;
            } else {
                levelTimeGradientWeight = remainingTimeGradient;
                remainingTimeGradient = 0.0;
            }
            var totalDiffLevel = totalDiffNumbers[i].doubleValue();
            var scoreDiffLevel = scoreDiffNumbers[i].doubleValue();
            if (scoreDiffLevel == totalDiffLevel) {
                // Max out this level
                timeGradient += levelTimeGradientWeight;
            } else if (scoreDiffLevel > totalDiffLevel) {
                // Max out this level and all softer levels too
                timeGradient += levelTimeGradientWeight + remainingTimeGradient;
                break;
            } else if (scoreDiffLevel == 0.0) {
                // Ignore this level
                // timeGradient += 0.0
            } else if (scoreDiffLevel < 0.0) {
                // Ignore this level and all softer levels too
                // timeGradient += 0.0
                break;
            } else {
                var levelTimeGradient = scoreDiffLevel / totalDiffLevel;
                timeGradient += levelTimeGradient * levelTimeGradientWeight;
            }

        }
        if (timeGradient > 1.0) {
            // Rounding error due to calculating with doubles
            timeGradient = 1.0;
        }
        return timeGradient;
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
        return "BestScore(" + bestScoreLimit + ")";
    }

}
