package ai.timefold.solver.benchmark.impl.result;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SolverBenchmarkResultTest {

    private static SolverBenchmarkResult buildSolverBenchmarkResult(SolverConfig solverConfig) {
        var solverBenchmarkResult = new SolverBenchmarkResult(new PlannerBenchmarkResult());
        solverBenchmarkResult.setName("Solver X");
        solverBenchmarkResult.setSolverConfig(solverConfig);
        return solverBenchmarkResult;
    }

    private static SolverConfig buildSolverConfig() {
        return new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(), new LocalSearchPhaseConfig());
    }

    @Test
    void environmentModeOfAConfigWithoutPhases() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var solverBenchmarkResult = buildSolverBenchmarkResult(solverConfig);
        assertThat(solverBenchmarkResult.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverBenchmarkResult.getStrictestEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverBenchmarkResult.getEnvironmentModeLabel()).isEqualTo("FULL_ASSERT");
    }

    @Test
    void environmentModeReflectsTheAdoptionOfAUnanimousPhaseMode() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList()
                .forEach(phaseConfig -> phaseConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        var solverBenchmarkResult = buildSolverBenchmarkResult(solverConfig);
        // The config declares PHASE_ASSERT, but every phase overrides it, so that is the mode the solver adopts.
        assertThat(solverBenchmarkResult.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverBenchmarkResult.getStrictestEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        // Nothing runs in a mode other than the adopted one, so there is no override left to report.
        assertThat(solverBenchmarkResult.getEnvironmentModeLabel()).isEqualTo("FULL_ASSERT");
    }

    @Test
    void strictestEnvironmentModeReflectsASingleOverridingPhase() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        var solverBenchmarkResult = buildSolverBenchmarkResult(solverConfig);
        assertThat(solverBenchmarkResult.getEnvironmentMode()).isEqualTo(EnvironmentMode.PHASE_ASSERT);
        assertThat(solverBenchmarkResult.getStrictestEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(solverBenchmarkResult.getEnvironmentModeLabel())
                .isEqualTo("PHASE_ASSERT (localSearch: FULL_ASSERT)");
    }

    @Test
    void environmentModeLabelNamesEveryOverridingPhase() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT);
        var solverBenchmarkResult = buildSolverBenchmarkResult(solverConfig);
        assertThat(solverBenchmarkResult.getEnvironmentModeLabel())
                .isEqualTo("STEP_ASSERT (constructionHeuristic: FULL_ASSERT, localSearch: TRACKED_FULL_ASSERT)");
    }

    @Test
    void environmentModeLabelDisambiguatesRepeatedPhaseTypes() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new ConstructionHeuristicPhaseConfig().withEnvironmentMode(EnvironmentMode.FULL_ASSERT),
                        new LocalSearchPhaseConfig());
        var solverBenchmarkResult = buildSolverBenchmarkResult(solverConfig);
        assertThat(solverBenchmarkResult.getEnvironmentModeLabel())
                .isEqualTo("PHASE_ASSERT (constructionHeuristic #2: FULL_ASSERT)");
    }

}
