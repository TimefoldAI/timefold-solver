package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertCompareToEquals;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertCompareToOrder;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.SingleBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.definition.SimpleScoreDefinition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotalRankSolverRankingWeightFactoryTest extends AbstractSolverRankingComparatorTest {

    private BenchmarkReport benchmarkReport;
    private TotalRankSolverRankingWeightFactory factory;
    private List<SolverBenchmarkResult> solverBenchmarkResultList;
    private SolverBenchmarkResult a;
    private SolverBenchmarkResult b;
    private List<SingleBenchmarkResult> aSingleBenchmarkResultList;
    private List<SingleBenchmarkResult> bSingleBenchmarkResultList;

    @BeforeEach
    void setUp() {
        benchmarkReport = mock(BenchmarkReport.class);
        factory = new TotalRankSolverRankingWeightFactory();
        solverBenchmarkResultList = new ArrayList<>();
        a = new SolverBenchmarkResult(null);
        a.setScoreDefinition(new SimpleScoreDefinition());
        b = new SolverBenchmarkResult(null);
        b.setScoreDefinition(new SimpleScoreDefinition());
        aSingleBenchmarkResultList = new ArrayList<>();
        bSingleBenchmarkResultList = new ArrayList<>();
    }

    @Test
    void normal() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -40, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -300, -40, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -40, -40, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -2000, -30, -2000); // Loses vs a
        addSingleBenchmark(b, bSingleBenchmarkResultList, -200, -30, -2000); // Wins vs a
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -2000); // Wins vs a
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        var totalSingleBenchmarkResultList = new ArrayList<>(aSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(bSingleBenchmarkResultList);
        addProblemBenchmark(totalSingleBenchmarkResultList);

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        assertCompareToOrder(aWeight, bWeight);
    }

    @Test
    void equalCount() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -5000, -90, -5000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -900, -90, -5000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -90, -90, -5000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList); // 0 wins - 1 equals - 5 losses
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -20, -1000); // Wins vs a - wins vs c
        addSingleBenchmark(b, bSingleBenchmarkResultList, -200, -20, -1000); // Wins vs a - loses vs c
        addSingleBenchmark(b, bSingleBenchmarkResultList, -20, -20, -1000); // Wins vs a - loses vs c
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList); // 4 wins - 0 equals - 2 losses
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        var c = new SolverBenchmarkResult(null);
        c.setScoreDefinition(new SimpleScoreDefinition());
        var cSingleBenchmarkResultList = new ArrayList<SingleBenchmarkResult>();
        addSingleBenchmark(c, cSingleBenchmarkResultList, -5000, -10, -5000); // Loses vs b, Equals vs a
        addSingleBenchmark(c, cSingleBenchmarkResultList, -100, -10, -5000); // Wins vs a - wins vs b
        addSingleBenchmark(c, cSingleBenchmarkResultList, -10, -10, -5000); // Wins vs a - wins vs b
        c.setSingleBenchmarkResultList(cSingleBenchmarkResultList); // 4 wins - 1 equals - 1 losses
        c.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(c);
        var totalSingleBenchmarkResultList = new ArrayList<>(aSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(bSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(cSingleBenchmarkResultList);
        addProblemBenchmark(totalSingleBenchmarkResultList);

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        var cWeight = factory.createRankingWeight(solverBenchmarkResultList, c);

        assertCompareToOrder(aWeight, bWeight, cWeight);
    }

    @Test
    void uninitializedSingleBenchmarks() {
        var a0 = addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        var b0 = addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        var b1 = addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        var totalSingleBenchmarkResultList = new ArrayList<>(aSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(bSingleBenchmarkResultList);
        addProblemBenchmark(totalSingleBenchmarkResultList);

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        assertCompareToEquals(aWeight, bWeight);

        a0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), false);
        a.accumulateResults(benchmarkReport);
        b.accumulateResults(benchmarkReport);
        // ranks, uninitialized variable counts, total scores and worst scores are equal
        assertCompareToEquals(aWeight, bWeight);

        b0.setAverageAndTotalScoreForTesting(SimpleScore.of(-1000), true);
        b1.setAverageAndTotalScoreForTesting(SimpleScore.of(-400), false);
        b.accumulateResults(benchmarkReport);
        // ranks, uninitialized variable counts and total scores are equal, A loses on worst score (tie-breaker)
        assertCompareToOrder(aWeight, bWeight);
    }

    @Test
    void differentNumberOfSingleBenchmarks() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        var totalSingleBenchmarkResultList = new ArrayList<>(aSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(bSingleBenchmarkResultList);
        addProblemBenchmark(totalSingleBenchmarkResultList);

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        assertCompareToOrder(aWeight, bWeight);
    }

    @Test
    void differentScoreTypeOfSingleBenchmarks() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        // Scores with different number of levels are compared from the highest level, see ResilientScoreComparator.compare
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, -1000, 0, -30, 0, -1000, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, -400, 0, -30, 0, -1000, -1000);
        addSingleBenchmarkWithHardSoftLongScore(b, bSingleBenchmarkResultList, -30, 0, -30, 0, -1000, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        var totalSingleBenchmarkResultList = new ArrayList<>(aSingleBenchmarkResultList);
        totalSingleBenchmarkResultList.addAll(bSingleBenchmarkResultList);
        addProblemBenchmark(totalSingleBenchmarkResultList);

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        assertCompareToEquals(aWeight, bWeight);
    }

    @Test
    void disjunctPlannnerBenchmarks() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -2000, -30, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -200, -30, -2000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -2000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        // A and B have different datasets (6 datasets in total)
        for (var solverBenchmarkResult : solverBenchmarkResultList) {
            for (var singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                addProblemBenchmark(Collections.singletonList(singleBenchmarkResult));
            }
        }

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        // Tie-breaker, A wins on total score
        assertCompareToOrder(bWeight, aWeight);
    }

    @Test
    void disjunctEqualPlannerBenchmarks() {
        addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        // A and B have different datasets (6 datasets in total)
        for (var solverBenchmarkResult : solverBenchmarkResultList) {
            for (var singleBenchmarkResult : solverBenchmarkResult.getSingleBenchmarkResultList()) {
                addProblemBenchmark(Collections.singletonList(singleBenchmarkResult));
            }
        }

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        // Tie-breaker (total score) is equal
        assertCompareToEquals(aWeight, bWeight);
    }

    @Test
    void overlappingPlannerBenchmarks() {
        var a0 = addSingleBenchmark(a, aSingleBenchmarkResultList, -1000, -30, -1000);
        var a1 = addSingleBenchmark(a, aSingleBenchmarkResultList, -400, -30, -1000);
        var a2 = addSingleBenchmark(a, aSingleBenchmarkResultList, -30, -30, -1000);
        a.setSingleBenchmarkResultList(aSingleBenchmarkResultList);
        a.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(a);
        var b0 = addSingleBenchmark(b, bSingleBenchmarkResultList, -1000, -30, -1000);
        var b1 = addSingleBenchmark(b, bSingleBenchmarkResultList, -400, -30, -1000);
        var b2 = addSingleBenchmark(b, bSingleBenchmarkResultList, -30, -30, -1000);
        b.setSingleBenchmarkResultList(bSingleBenchmarkResultList);
        b.accumulateResults(benchmarkReport);
        solverBenchmarkResultList.add(b);
        addProblemBenchmark(Arrays.asList(a0, b0));
        addProblemBenchmark(Arrays.asList(a1, b1));
        addProblemBenchmark(Arrays.asList(a2, b2));

        var aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        var bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        assertCompareToEquals(aWeight, bWeight);

        addProblemBenchmark(Collections.singletonList(a1));
        addProblemBenchmark(Collections.singletonList(a2));
        addProblemBenchmark(Collections.singletonList(b0));
        addProblemBenchmark(Collections.singletonList(b2));
        addProblemBenchmark(Arrays.asList(a0, b1));
        aWeight = factory.createRankingWeight(solverBenchmarkResultList, a);
        bWeight = factory.createRankingWeight(solverBenchmarkResultList, b);
        // A looses on score: a0 vs b1
        assertCompareToOrder(aWeight, bWeight);
    }

}
