package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToEquals;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorstScoreSolverRankingComparatorTest extends AbstractSolverRankingComparatorTest {

    private BenchmarkReport benchmarkReport;
    private WorstScoreSolverRankingComparator comparator;
    private SolverBenchmarkResult a;
    private SolverBenchmarkResult b;
    private List<SingleBenchmarkResult> aSingleBenchmarkResultList;
    private List<SingleBenchmarkResult> bSingleBenchmarkResultList;

    @BeforeEach
    void setUp() {
        benchmarkReport = mock(BenchmarkReport.class);
        comparator = new WorstScoreSolverRankingComparator();
        a = new SolverBenchmarkResult(null);
        a.setScoreDefinition(new SimpleScoreDefinition());
        b = new SolverBenchmarkResult(null);
        b.setScoreDefinition(new SimpleScoreDefinition());
        aSingleBenchmarkResultList = new ArrayList<>();
        bSingleBenchmarkResultList = new ArrayList<>();
    }

    @Test
    void normal() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -100, -30, -2001);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -2001, -30, -2001);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -2001);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -900, -30, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -2000, -30, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -2000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void worstIsEqual() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -101, -30, -2000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -2000, -30, -2000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -2000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -100, -40, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -2000, -40, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -40, -40, -2000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void differentScoreDefinitions() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, 0, -1000, 0, -50, -10, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, 0, -200, 0, -50, -10, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, -7, -50, 0, -50, -10, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

    @Test
    void uninitializedSingleBenchmarks() {
        var a0 = addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        var b0 = addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        var b1 = addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        assertCompareToEquals(comparator, a, b);

        a0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        b1.setAverageAndTotalScoreForTesting(SimpleScore.of(400), false);
        a.accumulateResults(benchmarkReport);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, b, a);

        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), true);
        b1.setAverageAndTotalScoreForTesting(SimpleScore.of(400), false);
        b.accumulateResults(benchmarkReport);
        assertCompareToOrder(comparator, a, b);
    }

}
