package ai.timefold.solver.examples.nqueens.optional.benchmark;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkException;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.examples.common.app.PlannerBenchmarkTest;
import ai.timefold.solver.examples.nqueens.app.NQueensApp;
import ai.timefold.solver.examples.nqueens.domain.NQueens;
import ai.timefold.solver.examples.nqueens.persistence.NQueensSolutionFileIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class BrokenNQueensBenchmarkTest extends PlannerBenchmarkTest {

    BrokenNQueensBenchmarkTest() {
        super(NQueensApp.SOLVER_CONFIG);
    }

    // ************************************************************************
    // Tests
    // ************************************************************************

    @Test
    @Timeout(100)
    void benchmarkBroken8queens() {
        NQueens problem = new NQueensSolutionFileIO()
                .read(new File("data/nqueens/unsolved/8queens.json"));
        PlannerBenchmarkConfig benchmarkConfig = buildPlannerBenchmarkConfig();
        benchmarkConfig.setWarmUpSecondsSpentLimit(0L);
        benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getTerminationConfig()
                .setStepCountLimit(-100); // Intentionally crash the solver
        PlannerBenchmark benchmark = PlannerBenchmarkFactory.create(benchmarkConfig).buildPlannerBenchmark(problem);
        assertThatExceptionOfType(PlannerBenchmarkException.class).isThrownBy(benchmark::benchmark);
    }

}
