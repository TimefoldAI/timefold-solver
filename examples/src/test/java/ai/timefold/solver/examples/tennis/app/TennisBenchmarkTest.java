package ai.timefold.solver.examples.tennis.app;

import java.io.File;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.examples.common.app.LoggingTest;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;
import ai.timefold.solver.examples.tennis.persistence.TennisGenerator;

import org.junit.jupiter.api.Test;

class TennisBenchmarkTest extends LoggingTest {

    @Test
    void benchmark() {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(TennisApp.SOLVER_CONFIG);
        PlannerBenchmarkConfig benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(
                solverConfig, new File("target/test/data/tennis"));
        benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig()
                .setTerminationConfig(new TerminationConfig().withScoreCalculationCountLimit(1000L));
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);

        TennisSolution problem = new TennisGenerator().createTennisSolution();
        PlannerBenchmark plannerBenchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        plannerBenchmark.benchmark();
    }

}
