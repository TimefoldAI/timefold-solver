package ai.timefold.solver.benchmark.impl.ranking;

import java.util.Comparator;

import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;

public class TotalScoreSingleBenchmarkRankingComparator implements Comparator<SingleBenchmarkResult> {

    @Override
    public int compare(SingleBenchmarkResult a, SingleBenchmarkResult b) {
        var aScoreDefinition = a.getSolverBenchmarkResult().getScoreDefinition();
        return Comparator
                // Reverse, less is better (redundant: failed benchmarks don't get ranked at all)
                .comparing(SingleBenchmarkResult::hasAnyFailure, Comparator.reverseOrder())
                .thenComparing(SingleBenchmarkResult::isInitialized)
                .thenComparing(SingleBenchmarkResult::getTotalScore, new ResilientScoreComparator(aScoreDefinition))
                .compare(a, b);
    }

}
