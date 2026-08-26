package ai.timefold.solver.benchmark.impl.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ai.timefold.solver.benchmark.impl.loader.FileProblemProvider;
import ai.timefold.solver.benchmark.impl.ranking.TotalScoreSolverRankingComparator;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class PlannerBenchmarkResultTest {

    private static final String TEST_PLANNER_BENCHMARK_RESULT = "testPlannerBenchmarkResult.xml";

    @Test
    void createMergedResult() {
        var p1 = new PlannerBenchmarkResult();
        p1.initSystemProperties();
        var p2 = new PlannerBenchmarkResult();
        p2.initSystemProperties();

        var p1SolverX = new SolverBenchmarkResult(p1);
        p1SolverX.setName("Solver X");
        var p1SolverConfigX = new SolverConfig();
        p1SolverConfigX.setRandomSeed(0L);
        p1SolverX.setSolverConfig(p1SolverConfigX);
        p1SolverX.setSingleBenchmarkResultList(new ArrayList<>());
        var p1SolverY = new SolverBenchmarkResult(p1);
        p1SolverY.setName("Solver Y");
        var p1SolverConfigY = new SolverConfig();
        p1SolverConfigY.setRandomSeed(1L);
        p1SolverY.setSolverConfig(p1SolverConfigY);
        p1SolverY.setSingleBenchmarkResultList(new ArrayList<>());
        var p2SolverZ = new SolverBenchmarkResult(p2);
        p2SolverZ.setName("Solver Z");
        var p2SolverConfigZ = new SolverConfig();
        p2SolverConfigZ.setRandomSeed(2L);
        p2SolverZ.setSolverConfig(p2SolverConfigZ);
        p2SolverZ.setSingleBenchmarkResultList(new ArrayList<>());

        var p1ProblemA = new ProblemBenchmarkResult<>(p1);
        p1ProblemA.setProblemProvider(new FileProblemProvider<>(null, new File("problemA.xml")));
        p1ProblemA.setProblemStatisticList(Collections.emptyList());
        p1ProblemA.setSingleBenchmarkResultList(Collections.emptyList());
        p1ProblemA.setSingleBenchmarkResultList(new ArrayList<>());
        var p1ProblemB = new ProblemBenchmarkResult<>(p1);
        p1ProblemB.setProblemProvider(new FileProblemProvider<>(null, new File("problemB.xml")));
        p1ProblemB.setProblemStatisticList(Collections.emptyList());
        p1ProblemB.setSingleBenchmarkResultList(Collections.emptyList());
        p1ProblemB.setSingleBenchmarkResultList(new ArrayList<>());
        var p2ProblemA = new ProblemBenchmarkResult<>(p2);
        p2ProblemA.setProblemProvider(new FileProblemProvider<>(null, new File("problemA.xml")));
        p2ProblemA.setProblemStatisticList(Collections.emptyList());
        p2ProblemA.setSingleBenchmarkResultList(Collections.emptyList());
        p2ProblemA.setSingleBenchmarkResultList(new ArrayList<>());

        var p1SolverXProblemA = createSingleBenchmarkResult(p1SolverX, p1ProblemA, -1);
        createSubSingleBenchmarkResult(p1SolverXProblemA, 1);
        var p1SolverXProblemB = createSingleBenchmarkResult(p1SolverX, p1ProblemB, -20);
        var p1SolverYProblemA = createSingleBenchmarkResult(p1SolverY, p1ProblemA, -300);
        var p1SolverYProblemB = createSingleBenchmarkResult(p1SolverY, p1ProblemB, -4000);
        var p2SolverZProblemA = createSingleBenchmarkResult(p2SolverZ, p2ProblemA, -50000);

        var mergedResult = PlannerBenchmarkResult.createMergedResult(
                Arrays.asList(p1SolverXProblemA, p1SolverXProblemB, p1SolverYProblemA, p1SolverYProblemB, p2SolverZProblemA));

        assertThat(mergedResult.getAggregation()).isTrue();
        var mergedProblemBenchmarkResultList = mergedResult.getUnifiedProblemBenchmarkResultList();
        var mergedSolverBenchmarkResultList = mergedResult.getSolverBenchmarkResultList();
        assertThat(mergedSolverBenchmarkResultList).hasSize(3);
        assertThat(mergedSolverBenchmarkResultList.get(0).getName()).isEqualTo("Solver X");
        assertThat(mergedSolverBenchmarkResultList.get(1).getName()).isEqualTo("Solver Y");
        assertThat(mergedSolverBenchmarkResultList.get(2).getName()).isEqualTo("Solver Z");
        assertThat(mergedProblemBenchmarkResultList).hasSize(2);
        assertThat(mergedProblemBenchmarkResultList.get(0).getProblemProvider().getProblemName()).isEqualTo("problemA");
        assertThat(mergedProblemBenchmarkResultList.get(1).getProblemProvider().getProblemName()).isEqualTo("problemB");
    }

    @Test
    void solverBenchmarksDifferingOnlyInAPhaseEnvironmentMode() {
        var otherSolverConfig = buildSolverConfigWithPhases();
        // Both configs declare PHASE_ASSERT at the solver level; only the phase override tells them apart.
        otherSolverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var plannerBenchmarkResult = accumulate(otherSolverConfig, buildSolverConfigWithPhases());
        // Null is what the report renders as "Differs".
        assertThat(plannerBenchmarkResult.getEnvironmentModeLabel()).isNull();
    }

    @Test
    void solverBenchmarksSharingAnEnvironmentModeAggregateAsThatMode() {
        var plannerBenchmarkResult = accumulate(buildSolverConfigWithPhases(), buildSolverConfigWithPhases());
        assertThat(plannerBenchmarkResult.getEnvironmentModeLabel()).isEqualTo("PHASE_ASSERT");
    }

    private static SolverConfig buildSolverConfigWithPhases() {
        return new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new LocalSearchPhaseConfig());
    }

    private static PlannerBenchmarkResult accumulate(SolverConfig... solverConfigs) {
        var plannerBenchmarkResult = new PlannerBenchmarkResult();
        var solverBenchmarkResultList = new ArrayList<SolverBenchmarkResult>(solverConfigs.length);
        for (var i = 0; i < solverConfigs.length; i++) {
            var solverBenchmarkResult = new SolverBenchmarkResult(plannerBenchmarkResult);
            solverBenchmarkResult.setName("Solver " + (char) ('A' + i));
            solverBenchmarkResult.setSolverConfig(solverConfigs[i]);
            solverBenchmarkResult.setSingleBenchmarkResultList(new ArrayList<>());
            solverBenchmarkResultList.add(solverBenchmarkResult);
        }
        plannerBenchmarkResult.setSolverBenchmarkResultList(solverBenchmarkResultList);
        plannerBenchmarkResult.setUnifiedProblemBenchmarkResultList(new ArrayList<>());
        var benchmarkReport = new BenchmarkReport(plannerBenchmarkResult);
        benchmarkReport.setSolverRankingComparator(new TotalScoreSolverRankingComparator());
        plannerBenchmarkResult.accumulateResults(benchmarkReport);
        return plannerBenchmarkResult;
    }

    protected SingleBenchmarkResult createSingleBenchmarkResult(SolverBenchmarkResult solverBenchmarkResult,
            ProblemBenchmarkResult problemBenchmarkResult, int score) {
        var singleBenchmarkResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        solverBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(singleBenchmarkResult);
        singleBenchmarkResult.setAverageAndTotalScoreForTesting(SimpleScore.of(score), false);
        singleBenchmarkResult.setSubSingleBenchmarkResultList(new ArrayList<>(1));
        createSubSingleBenchmarkResult(singleBenchmarkResult, 0);
        return singleBenchmarkResult;
    }

    protected void createSubSingleBenchmarkResult(SingleBenchmarkResult parent, int subSingleIndex) {
        var subSingleBenchmarkResult = new SubSingleBenchmarkResult(parent, subSingleIndex);
        subSingleBenchmarkResult.setPureSubSingleStatisticList(Collections.emptyList());
        parent.getSubSingleBenchmarkResultList().add(subSingleBenchmarkResult);
    }

    @Test
    void xmlReportRemainsSameAfterReadWrite() throws IOException {
        var benchmarkResultIO = new BenchmarkResultIO();
        PlannerBenchmarkResult plannerBenchmarkResult;
        try (var reader = new InputStreamReader(
                PlannerBenchmarkResultTest.class.getResourceAsStream(TEST_PLANNER_BENCHMARK_RESULT), "UTF-8")) {
            plannerBenchmarkResult = benchmarkResultIO.read(reader);
        }

        var stringWriter = new StringWriter();
        benchmarkResultIO.write(plannerBenchmarkResult, stringWriter);
        var jaxbString = stringWriter.toString();

        var originalXml = IOUtils.toString(PlannerBenchmarkResultTest.class.getResourceAsStream(TEST_PLANNER_BENCHMARK_RESULT),
                StandardCharsets.UTF_8);

        assertThat(jaxbString.trim()).isEqualToIgnoringWhitespace(originalXml.trim());
    }

    // nested classes below are used in the testPlannerBenchmarkResult.xml

    private abstract static class DummyEasyScoreCalculator
            implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

    }

    private abstract static class DummyDistanceNearbyMeter
            implements NearbyDistanceMeter<TestdataSolution, TestdataEntity> {

    }
}
