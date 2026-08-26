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
 * this covers them at their new home, plus what the resolver adds on top for a reporting caller:
 * the per-phase view, the strictest mode, and the guarantee that neither throws.
 */
class EnvironmentModeResolverTest {

    private static SolverConfig buildSolverConfig() {
        return PlannerTestUtils.buildSolverConfig(TestdataSolution.class, TestdataEntity.class);
    }

    @Test
    void resolveWithoutPhasesIsTheDeclaredMode() {
        var solverConfig = new SolverConfig()
                .withSolutionClass(TestdataSolution.class)
                .withEntityClasses(TestdataEntity.class)
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolve(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolvePhases(solverConfig)).isEmpty();
        assertThat(EnvironmentModeResolver.resolveStrictest(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThatCode(() -> EnvironmentModeResolver.validate(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void resolveReflectsTheAdoptionOfAUnanimousPhaseMode() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList()
                .forEach(phaseConfig -> phaseConfig.setEnvironmentMode(EnvironmentMode.FULL_ASSERT));
        // The declared mode is still PHASE_ASSERT, but that is not the mode anything runs in.
        assertThat(solverConfig.determineEnvironmentMode()).isEqualTo(EnvironmentMode.PHASE_ASSERT);
        assertThat(EnvironmentModeResolver.resolve(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolvePhases(solverConfig))
                .containsExactly(EnvironmentMode.FULL_ASSERT, EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolveStrictest(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void oneOverridingPhaseLeavesTheGlobalModeAloneButNotTheStrictestMode() {
        var solverConfig = buildSolverConfig();
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolve(solverConfig)).isEqualTo(EnvironmentMode.PHASE_ASSERT);
        assertThat(EnvironmentModeResolver.resolvePhases(solverConfig))
                .containsExactly(EnvironmentMode.PHASE_ASSERT, EnvironmentMode.FULL_ASSERT);
        // This is the whole point: the solver-level mode is cheap, yet half the run is not.
        assertThat(EnvironmentModeResolver.resolveStrictest(solverConfig)).isEqualTo(EnvironmentMode.FULL_ASSERT);
    }

    @Test
    void resolveTakesTheStrictestOfDifferingPhaseModes() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(0).setEnvironmentMode(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.TRACKED_FULL_ASSERT);
        assertThat(EnvironmentModeResolver.resolve(solverConfig)).isEqualTo(EnvironmentMode.STEP_ASSERT);
        assertThat(EnvironmentModeResolver.resolveStrictest(solverConfig))
                .isEqualTo(EnvironmentMode.TRACKED_FULL_ASSERT);
    }

    @Test
    void resolvingNeverThrowsOnConfigValidateRejects() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.STEP_ASSERT);
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.NO_ASSERT);
        assertThatCode(() -> EnvironmentModeResolver.validate(solverConfig))
                .hasMessageContaining("must have an assertion level higher than or equal to the global environment level");
        // A report runs long after the config was accepted; it must never be able to fail on validation.
        assertThatCode(() -> EnvironmentModeResolver.resolve(solverConfig)).doesNotThrowAnyException();
        assertThatCode(() -> EnvironmentModeResolver.resolvePhases(solverConfig)).doesNotThrowAnyException();
        assertThatCode(() -> EnvironmentModeResolver.resolveStrictest(solverConfig)).doesNotThrowAnyException();
    }

    @Test
    void nonReproducibleGlobalModeAdmitsNoPhaseOverride() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        // A stricter override is rejected just like a more lenient one: NON_REPRODUCIBLE reseeds on every run,
        // so a phase-level override would have nothing reproducible to be an override of.
        solverConfig.getPhaseConfigList().get(1).setEnvironmentMode(EnvironmentMode.FULL_ASSERT);
        assertThatCode(() -> EnvironmentModeResolver.validate(solverConfig))
                .hasMessageContaining("is only possible when global environmentMode is reproducible");
    }

    @Test
    void nonReproducibleGlobalModeIsValidWhileNoPhaseOverridesIt() {
        var solverConfig = buildSolverConfig()
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        // Restating the mode a phase already runs in is not an override, so the rule leaves it alone.
        solverConfig.getPhaseConfigList().getFirst().setEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE);
        assertThatCode(() -> EnvironmentModeResolver.validate(solverConfig)).doesNotThrowAnyException();
        assertThat(EnvironmentModeResolver.resolve(solverConfig)).isEqualTo(EnvironmentMode.NON_REPRODUCIBLE);
        assertThat(EnvironmentModeResolver.resolveStrictest(solverConfig)).isEqualTo(EnvironmentMode.NON_REPRODUCIBLE);
    }

}
