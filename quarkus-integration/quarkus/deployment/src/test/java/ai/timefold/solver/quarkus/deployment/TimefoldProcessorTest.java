package ai.timefold.solver.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.deployment.config.TimefoldBuildTimeConfig;

import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

class TimefoldProcessorTest {

    @Test
    void customScoreDrl_overrides_solverConfig() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withScoreDrls("config_constraints.drl");
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldProcessor timefoldProcessor = mockTimefoldProcessor();
        when(timefoldProcessor.constraintsDrl()).thenReturn(Optional.of("some.drl"));

        timefoldProcessor.applyScoreDirectorFactoryProperties(mock(IndexView.class), solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList()).containsExactly("some.drl");
    }

    @Test
    void defaultScoreDrl_does_not_override_solverConfig() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig()
                .withScoreDrls("config_constraints.drl");
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldProcessor timefoldProcessor = mockTimefoldProcessor();
        when(timefoldProcessor.constraintsDrl()).thenReturn(Optional.empty());
        when(timefoldProcessor.defaultConstraintsDrl())
                .thenReturn(Optional.of(TimefoldBuildTimeConfig.DEFAULT_CONSTRAINTS_DRL_URL));

        timefoldProcessor.applyScoreDirectorFactoryProperties(mock(IndexView.class), solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList())
                .containsExactly("config_constraints.drl");
    }

    @Test
    void defaultScoreDrl_applies_if_solverConfig_does_not_define_scoreDrl() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldProcessor timefoldProcessor = mockTimefoldProcessor();
        when(timefoldProcessor.constraintsDrl()).thenReturn(Optional.empty());
        when(timefoldProcessor.defaultConstraintsDrl())
                .thenReturn(Optional.of(TimefoldBuildTimeConfig.DEFAULT_CONSTRAINTS_DRL_URL));

        timefoldProcessor.applyScoreDirectorFactoryProperties(mock(IndexView.class), solverConfig);
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList())
                .containsExactly(TimefoldBuildTimeConfig.DEFAULT_CONSTRAINTS_DRL_URL);
    }

    @Test
    void error_if_kie_base_configuration_properties_is_present() {
        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setKieBaseConfigurationProperties(Collections.emptyMap());
        SolverConfig solverConfig = new SolverConfig().withScoreDirectorFactory(scoreDirectorFactoryConfig);
        TimefoldProcessor timefoldProcessor = mockTimefoldProcessor();
        when(timefoldProcessor.constraintsDrl()).thenReturn(Optional.empty());
        when(timefoldProcessor.defaultConstraintsDrl())
                .thenReturn(Optional.of(TimefoldBuildTimeConfig.DEFAULT_CONSTRAINTS_DRL_URL));

        assertThatCode(() -> timefoldProcessor.applyScoreDirectorFactoryProperties(mock(IndexView.class), solverConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Using kieBaseConfigurationProperties ("
                                + scoreDirectorFactoryConfig.getKieBaseConfigurationProperties()
                                + ") in Quarkus, which is unsupported.");
        assertThat(scoreDirectorFactoryConfig.getScoreDrlList())
                .containsExactly(TimefoldBuildTimeConfig.DEFAULT_CONSTRAINTS_DRL_URL);
    }

    private TimefoldProcessor mockTimefoldProcessor() {
        TimefoldProcessor timefoldProcessor = mock(TimefoldProcessor.class);
        doCallRealMethod().when(timefoldProcessor).applyScoreDirectorFactoryProperties(any(), any());
        return timefoldProcessor;
    }
}
