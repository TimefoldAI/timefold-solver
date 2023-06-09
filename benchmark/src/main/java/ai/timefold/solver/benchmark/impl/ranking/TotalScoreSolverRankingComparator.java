package ai.timefold.solver.benchmark.impl.ranking;

import java.util.Comparator;

import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

/**
 * This ranking {@link Comparator} orders a {@link SolverBenchmarkResult} by its total {@link Score}.
 * It maximize the overall score, so it minimizes the overall cost if all {@link PlanningSolution}s would be executed.
 * <p>
 * When the inputSolutions differ greatly in size or difficulty, this often results in a big difference in
 * {@link Score} magnitude between each {@link PlanningSolution}. For example: score 10 for dataset A versus 1000 for dataset B.
 * In such cases, dataset B would marginalize dataset A.
 * To avoid that, use {@link TotalRankSolverRankingWeightFactory}.
 */
public class TotalScoreSolverRankingComparator implements Comparator<SolverBenchmarkResult> {

    private final Comparator<SolverBenchmarkResult> worstScoreSolverRankingComparator = new WorstScoreSolverRankingComparator();

    @Override
    public int compare(SolverBenchmarkResult a, SolverBenchmarkResult b) {
        ScoreDefinition aScoreDefinition = a.getScoreDefinition();
        return Comparator
                .comparing(SolverBenchmarkResult::getFailureCount, Comparator.reverseOrder())
                .thenComparing(SolverBenchmarkResult::getTotalScore,
                        new ResilientScoreComparator(aScoreDefinition))
                .thenComparing(worstScoreSolverRankingComparator)
                .compare(a, b);

    }

}
