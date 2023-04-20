package ai.timefold.solver.benchmark.impl.ranking;

import java.util.Comparator;

import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

public class ScoreSubSingleBenchmarkRankingComparator implements Comparator<SubSingleBenchmarkResult> {

    @Override
    public int compare(SubSingleBenchmarkResult a, SubSingleBenchmarkResult b) {
        ScoreDefinition<?> aScoreDefinition = a.getSingleBenchmarkResult().getSolverBenchmarkResult().getScoreDefinition();
        return Comparator
                // Reverse, less is better (redundant: failed benchmarks don't get ranked at all)
                .comparing(SubSingleBenchmarkResult::hasAnyFailure, Comparator.reverseOrder())
                .thenComparing(SubSingleBenchmarkResult::getScore,
                        new ResilientScoreComparator(aScoreDefinition))
                .compare(a, b);
    }

}
