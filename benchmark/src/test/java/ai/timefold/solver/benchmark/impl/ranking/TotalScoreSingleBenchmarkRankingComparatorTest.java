package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;

import org.junit.jupiter.api.Test;

class TotalScoreSingleBenchmarkRankingComparatorTest {

    @Test
    void compareTo() {
        SolverBenchmarkResult solverBenchmarkResult = mock(SolverBenchmarkResult.class);
        when(solverBenchmarkResult.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        TotalScoreSingleBenchmarkRankingComparator comparator = new TotalScoreSingleBenchmarkRankingComparator();
        SingleBenchmarkResult a = new SingleBenchmarkResult(solverBenchmarkResult, mock(ProblemBenchmarkResult.class));
        a.setFailureCount(1);
        a.setAverageAndTotalScoreForTesting(null);
        SingleBenchmarkResult b = new SingleBenchmarkResult(solverBenchmarkResult, mock(ProblemBenchmarkResult.class));
        b.setFailureCount(0);
        b.setAverageAndTotalScoreForTesting(SimpleScore.ofUninitialized(-7, -1));
        SingleBenchmarkResult c = new SingleBenchmarkResult(solverBenchmarkResult, mock(ProblemBenchmarkResult.class));
        c.setFailureCount(0);
        c.setAverageAndTotalScoreForTesting(SimpleScore.of(-300));
        when(solverBenchmarkResult.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        SingleBenchmarkResult d = new SingleBenchmarkResult(solverBenchmarkResult, mock(ProblemBenchmarkResult.class));
        d.setFailureCount(0);
        d.setAverageAndTotalScoreForTesting(SimpleScore.of(-20));
        assertCompareToOrder(comparator, a, b, c, d);
    }

}
