package ai.timefold.solver.benchmark.impl.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.benchmark.impl.result.PlannerBenchmarkResult;
import ai.timefold.solver.benchmark.impl.result.SolverBenchmarkResult;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BenchmarkReportTest {

    private static SolverConfig buildSolverConfig() {
        return new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new LocalSearchPhaseConfig());
    }

    private static List<String> buildEnvironmentModeWarningList(SolverConfig... solverConfigs) {
        var plannerBenchmarkResult = new PlannerBenchmarkResult();
        var solverBenchmarkResultList = new ArrayList<SolverBenchmarkResult>(solverConfigs.length);
        for (var i = 0; i < solverConfigs.length; i++) {
            var solverBenchmarkResult = new SolverBenchmarkResult(plannerBenchmarkResult);
            solverBenchmarkResult.setName("Solver A %d".formatted(i));
            solverBenchmarkResult.setSolverConfig(solverConfigs[i]);
            solverBenchmarkResultList.add(solverBenchmarkResult);
        }
        plannerBenchmarkResult.setSolverBenchmarkResultList(solverBenchmarkResultList);
        // Only the environmentMode warnings are of interest; the others depend on the machine running the test.
        return new BenchmarkReport(plannerBenchmarkResult).getWarningList().stream()
                .filter(warning -> warning.contains("environmentMode"))
                .toList();
    }

    @Test
    void stepAssertingPhaseIsWarnedAboutAndNamed() {
        var solverConfig = buildSolverConfig();
        // The scenario this guards: a leftover debugging override on one phase of one solver benchmark,
        // which handicaps it against its peers while the solver-level mode says everything is fine.
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var warningList = buildEnvironmentModeWarningList(solverConfig);
        assertThat(warningList).hasSize(1);
        assertThat(warningList.getFirst())
                .contains("PHASE_ASSERT (localSearch: FULL_ASSERT)")
                .contains("Solver A")
                .contains("step-asserting or more");
    }

    @Test
    void phaseAssertingSolverIsNotWarnedAbout() {
        assertThat(buildEnvironmentModeWarningList(buildSolverConfig())).isEmpty();
    }

    @Test
    void stepAssertingSolverLevelModeIsStillWarnedAbout() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var warningList = buildEnvironmentModeWarningList(solverConfig);
        assertThat(warningList).hasSize(1);
        assertThat(warningList.getFirst())
                .contains("FULL_ASSERT")
                .contains("step-asserting or more");
    }

    @Test
    void solverBenchmarksInDifferentEnvironmentModesAreWarnedAboutAsIncomparable() {
        var otherSolverConfig = buildSolverConfig();
        otherSolverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        // Both solver benchmarks declare PHASE_ASSERT at the solver level, so only the phase override tells them apart.
        assertThat(buildEnvironmentModeWarningList(otherSolverConfig, buildSolverConfig()))
                .anySatisfy(warning -> assertThat(warning)
                        .contains("do not all run in the same environmentMode")
                        .contains("PHASE_ASSERT (localSearch: FULL_ASSERT)")
                        .contains("Their results are not comparable"));
    }

    @Test
    void solverBenchmarksInTheSameEnvironmentModeAreNotWarnedAboutAsIncomparable() {
        assertThat(buildEnvironmentModeWarningList(buildSolverConfig(), buildSolverConfig())).isEmpty();
    }

}
