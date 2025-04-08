package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.benchmark.impl.result.ProblemBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SubSingleBenchmarkResult;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;

import org.junit.jupiter.api.Test;

class ScoreSubSingleBenchmarkRankingComparatorTest {

    @Test
    void compareTo() {
        var comparator = new ScoreSubSingleBenchmarkRankingComparator();
        var solverBenchmarkResult = mock(SolverBenchmarkResult.class);
        when(solverBenchmarkResult.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        var singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, mock(ProblemBenchmarkResult.class));
        var a = new SubSingleBenchmarkResult(singleBenchmarkResult, 0);
        a.setSucceeded(false);
        a.setScore(null, false);
        var b = new SubSingleBenchmarkResult(singleBenchmarkResult, 1);
        b.setSucceeded(true);
        b.setScore(SimpleScore.of(-1), false);
        var c = new SubSingleBenchmarkResult(singleBenchmarkResult, 2);
        c.setSucceeded(true);
        c.setScore(SimpleScore.of(-300), true);
        var d = new SubSingleBenchmarkResult(singleBenchmarkResult, 3);
        d.setSucceeded(true);
        d.setScore(SimpleScore.of(-20), true);
        assertCompareToOrder(comparator, a, b, c, d);
    }

}
