package ai.timefold.solver.benchmark.impl.ranking;

import java.util.List;

import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.ScoreDifferencePercentage;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

public abstract class AbstractSolverRankingComparatorTest {

    protected <Solution_> ProblemBenchmarkResult<Solution_>
            addProblemBenchmark(List<SingleBenchmarkResult> singleBenchmarkResultList) {
        ProblemBenchmarkResult<Solution_> problemBenchmarkResult = new ProblemBenchmarkResult<>(null);
        problemBenchmarkResult.setSingleBenchmarkResultList(singleBenchmarkResultList);
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            singleBenchmarkResult.setProblemBenchmarkResult(problemBenchmarkResult);
        }
        return problemBenchmarkResult;
    }

    protected SingleBenchmarkResult addSingleBenchmark(SolverBenchmarkResult solverBenchmarkResult,
            List<SingleBenchmarkResult> singleBenchmarkResultList, int score, int bestScore, int worstScore) {
        return addSingleBenchmark(solverBenchmarkResult, singleBenchmarkResultList,
                SimpleScore.of(score),
                SimpleScore.of(bestScore),
                SimpleScore.of(worstScore));
    }

    protected SingleBenchmarkResult addSingleBenchmarkWithHardSoftLongScore(SolverBenchmarkResult solverBenchmarkResult,
            List<SingleBenchmarkResult> singleBenchmarkResultList,
            long hardScore, long softScore, long hardBestScore, long softBestScore, long hardWorstScore, long softWorstScore) {
        return addSingleBenchmark(solverBenchmarkResult, singleBenchmarkResultList,
                HardSoftLongScore.of(hardScore, softScore),
                HardSoftLongScore.of(hardBestScore, softBestScore),
                HardSoftLongScore.of(hardWorstScore, softWorstScore));
    }

    protected <Score_ extends Score<Score_>> SingleBenchmarkResult addSingleBenchmark(
            SolverBenchmarkResult solverBenchmarkResult,
            List<SingleBenchmarkResult> singleBenchmarkResultList,
            Score_ score, Score_ bestScore, Score_ worstScore) {
        SingleBenchmarkResult singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, null);
        singleBenchmarkResult.setFailureCount(0);
        singleBenchmarkResult.setAverageAndTotalScoreForTesting(score);
        singleBenchmarkResult.setWinningScoreDifference(score.subtract(bestScore));
        singleBenchmarkResult.setWorstScoreDifferencePercentage(
                ScoreDifferencePercentage.calculateScoreDifferencePercentage(worstScore, score));
        singleBenchmarkResult.setWorstScoreCalculationSpeedDifferencePercentage(5.0);
        singleBenchmarkResultList.add(singleBenchmarkResult);
        return singleBenchmarkResult;
    }

}
