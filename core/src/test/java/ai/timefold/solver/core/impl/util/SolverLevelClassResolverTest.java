package ai.timefold.solver.core.impl.util;

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

class SolverLevelClassResolverTest {

    private static SolverConfig neighborhoodsSolverConfig(LocalSearchPhaseConfig... phaseConfigs) {
        return new SolverConfig()
                .withPreviewFeature(PreviewFeature.NEIGHBORHOODS)
                .withPhases(phaseConfigs);
    }

    @Test
    void oneClassSetsSolverLevel() {
        var solverConfig = neighborhoodsSolverConfig();
        SolverLevelClassResolver.MOVE_SELECTION_STRATEGY.resolve(Map.of("solver", solverConfig),
                () -> List.of(FixedWeightMoveSelectionStrategy.class));
        assertThat(solverConfig.getNeighborhoodsConfig().getMoveSelectionStrategyClass())
                .isEqualTo(FixedWeightMoveSelectionStrategy.class);
    }

    @Test
    void oneClassSetsEverySolverThatNeedsIt() {
        var solverConfig1 = neighborhoodsSolverConfig();
        var solverConfig2 = neighborhoodsSolverConfig();
        var solverConfigMap = Map.of("solver1", solverConfig1, "solver2", solverConfig2);
        SolverLevelClassResolver.NEIGHBORHOOD_PROVIDER.resolve(solverConfigMap,
                () -> List.of(DefaultNeighborhoodProvider.class));
        SolverLevelClassResolver.MOVE_SELECTION_STRATEGY.resolve(solverConfigMap,
                () -> List.of(FixedWeightMoveSelectionStrategy.class));
        for (var solverConfig : List.of(solverConfig1, solverConfig2)) {
            assertThat(solverConfig.getNeighborhoodsConfig().getNeighborhoodProviderClass())
                    .isEqualTo(DefaultNeighborhoodProvider.class);
            assertThat(solverConfig.getNeighborhoodsConfig().getMoveSelectionStrategyClass())
                    .isEqualTo(FixedWeightMoveSelectionStrategy.class);
        }
    }

    @Test
    void ignoredWithoutNeighborhoods() {
        var solverConfig = new SolverConfig();
        SolverLevelClassResolver.MOVE_SELECTION_STRATEGY.resolve(Map.of("solver", solverConfig), () -> {
            throw new IllegalStateException("Not called without NEIGHBORHOODS.");
        });
        assertThat(solverConfig.getNeighborhoodsConfig()).isNull();
    }

    @Test
    void strategyNotSetWithMoveSelectorPhase() {
        var solverConfig = neighborhoodsSolverConfig(
                new LocalSearchPhaseConfig().withMoveSelectorConfig(new ChangeMoveSelectorConfig()),
                new LocalSearchPhaseConfig());
        SolverLevelClassResolver.MOVE_SELECTION_STRATEGY.resolve(Map.of("solver", solverConfig),
                () -> List.of(FixedWeightMoveSelectionStrategy.class));
        assertThat(solverConfig.getNeighborhoodsConfig()).isNull();

        // The neighborhood provider also applies to phases with move selectors.
        SolverLevelClassResolver.NEIGHBORHOOD_PROVIDER.resolve(Map.of("solver", solverConfig),
                () -> List.of(DefaultNeighborhoodProvider.class));
        assertThat(solverConfig.getNeighborhoodsConfig().getNeighborhoodProviderClass())
                .isEqualTo(DefaultNeighborhoodProvider.class);
        assertThat(solverConfig.getNeighborhoodsConfig().getMoveSelectionStrategyClass()).isNull();
    }

    @Test
    void multipleClassesFail() {
        var solverConfig = neighborhoodsSolverConfig();
        assertThatIllegalStateException()
                .isThrownBy(() -> SolverLevelClassResolver.MOVE_SELECTION_STRATEGY.resolve(Map.of("solver", solverConfig),
                        () -> List.of(FixedWeightMoveSelectionStrategy.class, SubclassStrategy.class)))
                .withMessageContaining("Multiple classes");
    }

    public static final class SubclassStrategy<Solution_> extends FixedWeightMoveSelectionStrategy<Solution_> {
    }

}
