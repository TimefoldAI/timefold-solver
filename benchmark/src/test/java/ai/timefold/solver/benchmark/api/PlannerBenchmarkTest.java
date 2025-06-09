package ai.timefold.solver.benchmark.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.benchmark.impl.DefaultPlannerBenchmark;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.testdomain.TestdataConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlannerBenchmarkTest {

    @Test
    void runPlannerBenchmark(@TempDir Path benchmarkTestDir) {
        var benchmarkConfig = new PlannerBenchmarkConfig();
        benchmarkConfig.setBenchmarkDirectory(benchmarkTestDir.toFile());
        benchmarkConfig.setWarmUpMillisecondsSpentLimit(1L); // Minimize warmup.
        var inheritedSolverConfig = new SolverBenchmarkConfig();
        inheritedSolverConfig.setSolverConfig(new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withConstraintProviderClass(TestdataConstraintProvider.class)
                // Only run for a short amount of time.
                .withTerminationConfig(new TerminationConfig().withUnimprovedMillisecondsSpentLimit(100L)));
        benchmarkConfig.setInheritedSolverBenchmarkConfig(inheritedSolverConfig);
        benchmarkConfig.setSolverBenchmarkConfigList(List.of(new SolverBenchmarkConfig()));
        var benchmarkFactory = PlannerBenchmarkFactory.create(benchmarkConfig);

        var solution1 = new TestdataSolution("s1");
        solution1.setEntityList(Arrays.asList(new TestdataEntity("e1"), new TestdataEntity("e2"), new TestdataEntity("e3")));
        solution1.setValueList(Arrays.asList(new TestdataValue("v1"), new TestdataValue("v2")));

        var plannerBenchmark = (DefaultPlannerBenchmark) benchmarkFactory.buildPlannerBenchmark(solution1);
        plannerBenchmark.benchmark(); // Run the benchmark.
        var folder = plannerBenchmark.getBenchmarkReport().getHtmlOverviewFile()
                .toPath()
                .getParent();
        var csv = folder.resolve(Path.of("Problem_0", "Config_0", "sub0", "BEST_SCORE.csv"));
        assertThat(csv).exists();

        try (var lines = Files.lines(csv)) {
            var lineList = lines.toList();
            assertThat(lineList).isNotEmpty();
            assertSoftly(softly -> {
                // Proper header.
                softly.assertThat(lineList)
                        .first()
                        .isEqualTo("""
                                "timeMillisSpent","score","initialized"
                                """.trim());
                // Checks that best score was recorded at least once.
                // Requires LS to have started, as CH does not store best score.
                // We only check score+initialized, as "timeMillisSpent" can be anything.
                softly.assertThat(lineList)
                        .last()
                        .asString()
                        .endsWith("""
                                "0","true"
                                """.trim());
            });
        } catch (IOException e) {
            fail(e);
        }
    }

}
