package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.neighborhood.DefaultNeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.selection.FixedWeightMoveSelectionStrategy;

import org.junit.jupiter.api.Test;

class SolverLevelClassDetectorTest {

    private static SolverConfig neighborhoodsSolverConfig(LocalSearchPhaseConfig... phaseConfigs) {
        return new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withPhases(phaseConfigs);
    }

    @Test
    void oneClassSetsSolverLevel() {
        var solverConfig = neighborhoodsSolverConfig();
        SolverLevelClassDetector.MOVE_SELECTION_STRATEGY.detect(Map.of("solver", solverConfig),
                () -> List.of(FixedWeightMoveSelectionStrategy.class));
        assertThat(solverConfig.getMoveSelectionStrategyClass()).isEqualTo(FixedWeightMoveSelectionStrategy.class);
    }

    @Test
    void ignoredWithoutNeighborhoods() {
        var solverConfig = new SolverConfig();
        SolverLevelClassDetector.MOVE_SELECTION_STRATEGY.detect(Map.of("solver", solverConfig), () -> {
            throw new IllegalStateException("Not called without NEIGHBORHOODS.");
        });
        assertThat(solverConfig.getMoveSelectionStrategyClass()).isNull();
    }

    @Test
    void strategyNotSetWithMoveSelectorPhase() {
        var solverConfig = neighborhoodsSolverConfig(
                new LocalSearchPhaseConfig().withMoveSelectorConfig(new ChangeMoveSelectorConfig()),
                new LocalSearchPhaseConfig());
        SolverLevelClassDetector.MOVE_SELECTION_STRATEGY.detect(Map.of("solver", solverConfig),
                () -> List.of(FixedWeightMoveSelectionStrategy.class));
        assertThat(solverConfig.getMoveSelectionStrategyClass()).isNull();

        // The neighborhood provider also applies to phases with move selectors.
        SolverLevelClassDetector.NEIGHBORHOOD_PROVIDER.detect(Map.of("solver", solverConfig),
                () -> List.of(DefaultNeighborhoodProvider.class));
        assertThat(solverConfig.getNeighborhoodProviderClass()).isEqualTo(DefaultNeighborhoodProvider.class);
    }

    @Test
    void multipleClassesFail() {
        var solverConfig = neighborhoodsSolverConfig();
        assertThatIllegalStateException()
                .isThrownBy(() -> SolverLevelClassDetector.MOVE_SELECTION_STRATEGY.detect(Map.of("solver", solverConfig),
                        () -> List.of(FixedWeightMoveSelectionStrategy.class, SubclassStrategy.class)))
                .withMessageContaining("Multiple classes");
    }

    public static final class SubclassStrategy<Solution_> extends FixedWeightMoveSelectionStrategy<Solution_> {
    }

}
