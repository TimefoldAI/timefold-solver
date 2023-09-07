package ai.timefold.solver.benchmark.impl;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.impl.report.BenchmarkReport;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class DefaultPlannerBenchmarkTest {

    @Test
    void benchmarkingStartedTwice() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(
                PlannerBenchmarkConfig.createFromSolverConfig(solverConfig));

        TestdataSolution solution = mock(TestdataSolution.class);

        DefaultPlannerBenchmark benchmark = (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(solution);
        benchmark.benchmarkingStarted();

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(benchmark::benchmarkingStarted).withNoCause();
    }

    @Test
    void solverBenchmarkResultListIsEmpty() {
        File benchmarkDirectory = mock(File.class);
        ExecutorService executorService = mock(ExecutorService.class);
        BenchmarkReport benchmarkReport = mock(BenchmarkReport.class);

        // solverBenchmarkResultList is empty when instantiated by default constructor
        PlannerBenchmarkResult benchmarkResult = new PlannerBenchmarkResult();

        DefaultPlannerBenchmark benchmark = new DefaultPlannerBenchmark(benchmarkResult,
                benchmarkDirectory, executorService,
                executorService, benchmarkReport);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(benchmark::benchmarkingStarted)
                .withMessageContaining("solverBenchmarkResultList").withMessageContaining("empty");
    }

    @Test
    void benchmarkDirectoryIsNull() {
        ExecutorService executorService = mock(ExecutorService.class);
        BenchmarkReport benchmarkReport = mock(BenchmarkReport.class);
        SolverBenchmarkResult benchmarkResult = mock(SolverBenchmarkResult.class);

        PlannerBenchmarkResult plannerBenchmarkResult = new PlannerBenchmarkResult();
        plannerBenchmarkResult.setSolverBenchmarkResultList(Collections.singletonList(benchmarkResult));

        DefaultPlannerBenchmark benchmark = new DefaultPlannerBenchmark(plannerBenchmarkResult, null,
                executorService, executorService, benchmarkReport);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(benchmark::benchmarkingStarted)
                .withMessageContaining("benchmarkDirectory").withMessageContaining("null");
    }

    @Test
    void exceptionMessagePropagatesWhenThrownDuringWarmUp() {
        SolverConfig solverConfig = PlannerTestUtils.buildSolverConfig(
                TestdataSolution.class, TestdataEntity.class);
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(
                PlannerBenchmarkConfig.createFromSolverConfig(solverConfig));

        TestdataSolution solution = mock(TestdataSolution.class);

        UnsupportedOperationException exception = new UnsupportedOperationException();
        when(solution.getEntityList()).thenThrow(exception);

        DefaultPlannerBenchmark benchmark = (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(solution);

        assertThatThrownBy(benchmark::benchmark)
                .hasRootCause(exception);
    }
}
