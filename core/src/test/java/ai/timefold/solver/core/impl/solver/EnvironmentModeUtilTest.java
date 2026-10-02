package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

/**
 * {@link DefaultSolverFactoryTest} covers the rules as the solver applies them, through a built solver;
 * this covers them at their new home, plus the per-phase view built on top of them.
 */
class EnvironmentModeUtilTest {

    private static SolverConfig buildSolverConfig() {
        return PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
    }

    @Test
    void resolveWithoutPhasesIsTheDeclaredMode() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeUtil.resolve(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true)).isEmpty();
        assertThatCode(() -> EnvironmentModeUtil.validate(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void everyPhaseOverridingLeavesTheGlobalModeAlone() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList()
                .forEach(phaseConfig -> phaseConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        // The global mode still stands, even though no phase runs in it:
        // it governs everything outside the phases, and the factory built for it is needed regardless.
        assertThat(EnvironmentModeUtil.resolve(solverConfig)).isEqualTo(EnvironmentMode.PHASE_ASSERT);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true))
                .containsExactly(EnvironmentMode.FULL_ASSERT, EnvironmentMode.FULL_ASSERT);
        assertThatCode(() -> EnvironmentModeUtil.validate(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void phaseWhichDoesNotOverrideUsesTheGlobalMode() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeUtil.resolve(solverConfig)).isEqualTo(EnvironmentMode.PHASE_ASSERT);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true))
                .containsExactly(EnvironmentMode.PHASE_ASSERT, EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void differingPhaseModesLeaveTheGlobalModeAlone() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT);
        assertThat(EnvironmentModeUtil.resolve(solverConfig)).isEqualTo(EnvironmentMode.STEP_ASSERT);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true))
                .containsExactly(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT, EnvironmentMode.TRACKED_FULL_ASSERT);
    }

    @Test
    void phaseLessStrictThanTheGlobalModeIsRejected() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NO_ASSERT);
        assertThatCode(() -> EnvironmentModeUtil.validate(solverConfig))
                .hasMessageContaining("must have an assertion level higher than or equal to the global environment level");
        // Reading the config is not validating it, so these stay usable on a config validate rejects.
        assertThatCode(() -> EnvironmentModeUtil.resolve(solverConfig)).doesNotThrowAnyException();
        assertThatCode(() -> EnvironmentModeUtil.resolvePhases(solverConfig, true)).doesNotThrowAnyException();
    }

    @Test
    void resolvePhasesWithTheGlobalEnvironmentMode() {
        // What the benchmark rule tests for: an empty result means no phase overrides the solver's mode.
        var solverConfig = buildSolverConfig();
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, false)).isEmpty();

        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        // Only the overriding phase is listed, unlike the useGlobalMode variant which describes every phase.
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, false))
                .containsExactly(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true))
                .containsExactly(EnvironmentMode.PHASE_ASSERT, EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void resolvePhasesWithEmptyPhaseList() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class);
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, false)).isEmpty();
        assertThat(EnvironmentModeUtil.resolvePhases(solverConfig, true)).isEmpty();
    }

    @Test
    void nonReproducibleGlobalModeAdmitsNoPhaseOverride() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        // A stricter override is rejected just like a more lenient one: NON_REPRODUCIBLE reseeds on every run,
        // so a phase-level override would have nothing reproducible to be an override of.
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThatCode(() -> EnvironmentModeUtil.validate(solverConfig))
                .hasMessageContaining("is only possible when global environmentMode is reproducible");
    }

    @Test
    void nonReproducibleGlobalModeIsValidWhileNoPhaseOverridesIt() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        // Restating the mode a phase already runs in is not an override, so the rule leaves it alone.
        solverConfig.getPhaseConfigList().getFirst().setEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        assertThatCode(() -> EnvironmentModeUtil.validate(solverConfig)).doesNotThrowAnyException();
        assertThat(EnvironmentModeUtil.resolve(solverConfig)).isEqualTo(EnvironmentMode.NON_REPRODUCIBLE);
    }

}
