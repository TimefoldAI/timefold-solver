package ai.timefold.solver.examples.common.app;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

public abstract class AbstractBenchmarkConfigTest {

    protected abstract CommonBenchmarkApp getBenchmarkApp();

    @TestFactory
    @Execution(ExecutionMode.CONCURRENT)
    Stream<DynamicTest> testBenchmarkApp() {
        return getBenchmarkApp().getArgOptions().stream()
                .map(argOption -> dynamicTest(argOption.toString(), () -> buildPlannerBenchmark(argOption)));
    }

    private static void buildPlannerBenchmark(CommonBenchmarkApp.ArgOption argOption) {
        String benchmarkConfigResource = argOption.getBenchmarkConfigResource();
        PlannerBenchmarkFactory benchmarkFactory;
        if (!argOption.isTemplate()) {
            benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource(benchmarkConfigResource);
        } else {
            benchmarkFactory = PlannerBenchmarkFactory.createFromFreemarkerXmlResource(benchmarkConfigResource);
        }
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark();
        buildEverySolver(benchmark);
    }

    private static void buildEverySolver(PlannerBenchmark plannerBenchmark) {
        PlannerBenchmarkResult plannerBenchmarkResult = ((DefaultPlannerBenchmark) plannerBenchmark)
                .getPlannerBenchmarkResult();
        for (SolverBenchmarkResult solverBenchmarkResult : plannerBenchmarkResult.getSolverBenchmarkResultList()) {
            SolverConfig solverConfig = solverBenchmarkResult.getSolverConfig();
            SolverFactory.create(solverConfig).buildSolver();
        }
    }
}
