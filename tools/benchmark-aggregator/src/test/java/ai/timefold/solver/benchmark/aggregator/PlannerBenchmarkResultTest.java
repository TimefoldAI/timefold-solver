package ai.timefold.solver.benchmark.aggregator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import ai.timefold.solver.benchmark.config.report.BenchmarkReportConfig;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReportFactory;
import ai.timefold.solver.benchmark.impl.result.BenchmarkResultIO;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.IncrementalScoreCalculator;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class PlannerBenchmarkResultTest {

    private static final String TEST_PLANNER_BENCHMARK_RESULT = "testPlannerBenchmarkResult.xml";

    @Test
    void xmlReadBenchmarkResultAggregated() throws URISyntaxException, IOException {
        var benchmarkAggregator = new BenchmarkAggregator();
        benchmarkAggregator.setBenchmarkDirectory(Files.createTempDirectory(getClass().getSimpleName()).toFile());
        benchmarkAggregator.setBenchmarkReportConfig(new BenchmarkReportConfig());

        var plannerBenchmarkResultFile =
                new File(PlannerBenchmarkResultTest.class.getResource(TEST_PLANNER_BENCHMARK_RESULT).toURI());

        var benchmarkResultIO = new BenchmarkResultIO();
        var plannerBenchmarkResult = benchmarkResultIO.readPlannerBenchmarkResult(plannerBenchmarkResultFile);

        var benchmarkReportConfig = benchmarkAggregator.getBenchmarkReportConfig();
        var benchmarkReport = new BenchmarkReportFactory(benchmarkReportConfig).buildBenchmarkReport(plannerBenchmarkResult);
        plannerBenchmarkResult.accumulateResults(benchmarkReport);

        var aggregatedPlannerBenchmarkResult = benchmarkReport.getPlannerBenchmarkResult();

        assertThat(aggregatedPlannerBenchmarkResult.getSolverBenchmarkResultList()).hasSize(6);
        assertThat(aggregatedPlannerBenchmarkResult.getUnifiedProblemBenchmarkResultList()).hasSize(2);
        assertThat(aggregatedPlannerBenchmarkResult.getFailureCount()).isZero();
    }

    // nested class below are used in the testPlannerBenchmarkResult.xml

    private static abstract class DummyIncrementalScoreCalculator
            implements IncrementalScoreCalculator<TestdataSolution, SimpleScore> {

    }

    private static abstract class DummyDistanceNearbyMeter
            implements NearbyDistanceMeter<TestdataSolution, TestdataEntity> {

    }
}
