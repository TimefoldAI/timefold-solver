package ai.timefold.solver.benchmark.impl.ranking;

import java.util.Comparator;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;

public class ScoreSubSingleBenchmarkRankingComparator implements Comparator<SubSingleBenchmarkResult> {

    @Override
    public int compare(SubSingleBenchmarkResult a, SubSingleBenchmarkResult b) {
        var aScoreDefinition = a.getSingleBenchmarkResult().getSolverBenchmarkResult().getScoreDefinition();
        return Comparator
                // Reverse, less is better (redundant: failed benchmarks don't get ranked at all)
                .comparing(SubSingleBenchmarkResult::hasAnyFailure, Comparator.reverseOrder())
                .thenComparing(SubSingleBenchmarkResult::isInitialized)
                .thenComparing(SubSingleBenchmarkResult::getScore,
                        new ResilientScoreComparator(aScoreDefinition))
                .compare(a, b);
    }

}
