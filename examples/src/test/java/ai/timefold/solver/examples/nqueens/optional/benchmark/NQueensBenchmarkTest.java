package ai.timefold.solver.examples.nqueens.optional.benchmark;

import java.io.File;
import java.util.Arrays;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.ProblemBenchmarksConfig;
import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType;
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.examples.common.app.PlannerBenchmarkTest;
import ai.timefold.solver.examples.nqueens.app.NQueensApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.persistence.NQueensSolutionFileIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class NQueensBenchmarkTest extends PlannerBenchmarkTest {

    private final NQueens problem = new NQueensSolutionFileIO()
            .read(new File("data/nqueens/unsolved/64queens.json"));

    NQueensBenchmarkTest() {
        super(NQueensApp.SOLVER_CONFIG);
    }

    // ************************************************************************
    // Tests
    // ************************************************************************

    @Test
    @Timeout(600)
    void benchmark64queens() {
        PlannerBenchmarkConfig benchmarkConfig = buildPlannerBenchmarkConfig();
        addAllStatistics(benchmarkConfig);
        benchmarkConfig.setParallelBenchmarkCount("AUTO");
        PlannerBenchmark benchmark = PlannerBenchmarkFactory.create(benchmarkConfig).buildPlannerBenchmark(problem);
        benchmark.benchmark();
    }

    @Test
    @Timeout(600)
    void benchmark64queensSingleThread() {
        PlannerBenchmarkConfig benchmarkConfig = buildPlannerBenchmarkConfig();
        addAllStatistics(benchmarkConfig);
        benchmarkConfig.setParallelBenchmarkCount("1");
        PlannerBenchmark benchmark = PlannerBenchmarkFactory.create(benchmarkConfig).buildPlannerBenchmark(problem);
        benchmark.benchmark();
    }

    protected void addAllStatistics(PlannerBenchmarkConfig benchmarkConfig) {
        ProblemBenchmarksConfig problemBenchmarksConfig = new ProblemBenchmarksConfig();
        problemBenchmarksConfig.setSingleStatisticTypeList(Arrays.asList(SingleStatisticType.values()));
        problemBenchmarksConfig.setProblemStatisticTypeList(Arrays.asList(ProblemStatisticType.values()));
        benchmarkConfig.getInheritedSolverBenchmarkConfig().setProblemBenchmarksConfig(problemBenchmarksConfig);
    }

    @Test
    void benchmarkDirectoryNameDuplication() {
        PlannerBenchmarkConfig benchmarkConfig = buildPlannerBenchmarkConfig();
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);
        DefaultPlannerBenchmark plannerBenchmark = (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(problem);
        plannerBenchmark.benchmarkingStarted();
        plannerBenchmark.getPlannerBenchmarkResult().initBenchmarkReportDirectory(benchmarkConfig.getBenchmarkDirectory());
        plannerBenchmark.getPlannerBenchmarkResult().initBenchmarkReportDirectory(benchmarkConfig.getBenchmarkDirectory());
    }

}
