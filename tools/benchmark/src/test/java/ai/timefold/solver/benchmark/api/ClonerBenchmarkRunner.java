package ai.timefold.solver.benchmark.api;

import java.io.File;
import java.util.List;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.core.api.domain.specification.PlanningSpecification;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

public class ClonerBenchmarkRunner {

    public static void main(String[] args) {
        // Build programmatic PlanningSpecification
        var spec = PlanningSpecification.of(TestdataSolution.class)
                .score(SimpleScore.class, TestdataSolution::getScore, TestdataSolution::setScore)
                .problemFacts("valueRange", TestdataSolution::getValueList)
                .entityCollection("entities", TestdataSolution::getEntityList)
                .valueRange("valueRange", TestdataSolution::getValueList)
                .entity(TestdataEntity.class, e -> e
                        .variable("value", TestdataValue.class, v -> v
                                .accessors(TestdataEntity::getValue, TestdataEntity::setValue)
                                .valueRange("valueRange")))
                .build();

        var termination = new TerminationConfig().withSecondsSpentLimit(30L);

        // Annotation-based solver config
        var annotationConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withTerminationConfig(termination);

        // Programmatic solver config
        var programmaticConfig = new SolverConfig()
                .withPlanningSpecification(spec)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                .withEnvironmentMode(EnvironmentMode.NO_ASSERT)
                .withTerminationConfig(termination);

        // Benchmark config with both solver benchmarks
        var benchmarkConfig = new PlannerBenchmarkConfig();
        benchmarkConfig.setBenchmarkDirectory(new File("local/benchmarkReport"));
        benchmarkConfig.setWarmUpSecondsSpentLimit(10L);

        var sb1 = new SolverBenchmarkConfig();
        sb1.setName("Annotation Path");
        sb1.setSolverConfig(annotationConfig);
        sb1.setSubSingleCount(3);

        var sb2 = new SolverBenchmarkConfig();
        sb2.setName("Programmatic API");
        sb2.setSolverConfig(programmaticConfig);
        sb2.setSubSingleCount(3);

        benchmarkConfig.setSolverBenchmarkConfigList(List.of(sb1, sb2));

        // Generate a large problem: 200 values, 800 entities
        var solution = TestdataSolution.generateSolution(200, 800);

        System.out.println("Starting benchmark: 200 values, 800 entities");
        System.out.println("Warm-up: 10s, Solving: 30s per config");
        System.out.println("Report will be at: local/benchmarkReport/");

        // Run benchmark
        var factory = PlannerBenchmarkFactory.create(benchmarkConfig);
        var benchmark = factory.buildPlannerBenchmark(solution);
        benchmark.benchmark();

        System.out.println("Benchmark complete. Open local/benchmarkReport/ for HTML report.");
    }
}
