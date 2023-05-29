package ai.timefold.solver.core.config.phase.custom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;

import org.junit.jupiter.api.Test;

class CustomPhaseConfigTest {

    private static final String SOLVER_CONFIG_RESOURCE =
            "ai/timefold/solver/core/config/phase/custom/testSolverConfigWithNonexistentCustomPhase.xml";

    @Test
    void nonExistentCustomPhaseCommand() {
        assertThatThrownBy(() -> SolverFactory.createFromXmlResource(SOLVER_CONFIG_RESOURCE)
                .buildSolver())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customPhaseCommandClass");
    }

    @Test
    void nullCustomPhaseCommands() {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(SOLVER_CONFIG_RESOURCE);
        assertThatThrownBy(() -> ((CustomPhaseConfig) solverConfig.getPhaseConfigList().get(0))
                .withCustomPhaseCommands(new CustomPhaseCommand[] { null }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Custom phase commands");
    }

    @Test
    void nullCustomPhaseCommandList() {
        SolverConfig solverConfig = SolverConfig.createFromXmlResource(SOLVER_CONFIG_RESOURCE);
        assertThatThrownBy(() -> ((CustomPhaseConfig) solverConfig.getPhaseConfigList().get(0))
                .withCustomPhaseCommandList(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Custom phase commands");
    }

}
