package ai.timefold.solver.examples.tennis.optional.benchmark;

import java.io.File;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.examples.common.app.LoggingMain;
import ai.timefold.solver.examples.tennis.app.TennisApp;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;
import ai.timefold.solver.examples.tennis.persistence.TennisGenerator;

public class TennisBenchmarkApp extends LoggingMain {

    public static void main(String[] args) {
        new TennisBenchmarkApp().benchmark();
    }

    private final PlannerBenchmarkFactory benchmarkFactory;

    public TennisBenchmarkApp() {
        benchmarkFactory = PlannerBenchmarkFactory.createFromSolverConfigXmlResource(
                TennisApp.SOLVER_CONFIG, new File("local/data/tennis"));
    }

    public void benchmark() {
        TennisSolution problem = new TennisGenerator().createTennisSolution();
        PlannerBenchmark plannerBenchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        plannerBenchmark.benchmark();
    }

}
