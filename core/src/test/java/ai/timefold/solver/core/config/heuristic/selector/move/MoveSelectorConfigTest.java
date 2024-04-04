package ai.timefold.solver.core.config.heuristic.selector.move;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.CartesianProductMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveIteratorFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.factory.MoveListFactoryConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.PillarSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.KOptMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.SubChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.testdata.domain.list.TestDistanceMeter;

import org.junit.jupiter.api.Test;

class MoveSelectorConfigTest {

    @Test
    void assertEnableNearbyForChangeMoveSelectorConfig() {
        // Default configuration
        ChangeMoveSelectorConfig config = new ChangeMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        ChangeMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();

        // Custom configuration
        config = new ChangeMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();
    }

    @Test
    void assertEnableNearbyForSwapMoveSelectorConfig() {
        // Default configuration
        SwapMoveSelectorConfig config = new SwapMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        SwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        // Custom configuration
        config = new SwapMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();
    }

    @Test
    void assertEnableNearbyForTailChainMoveSelectorConfig() {
        // Default configuration
        TailChainSwapMoveSelectorConfig config = new TailChainSwapMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        TailChainSwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();

        // Custom configuration
        config = new TailChainSwapMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getEntitySelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();
    }

    @Test
    void assertEnableNearbyForListChangeMoveSelectorConfig() {
        // Default configuration
        ListChangeMoveSelectorConfig config = new ListChangeMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        ListChangeMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getOriginValueSelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        // Custom configuration
        config = new ListChangeMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getValueSelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getOriginValueSelectorConfig())
                .isNotNull();
        assertThat(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();
    }

    @Test
    void assertEnableNearbyForListSwapMoveSelectorConfig() {
        // Default configuration
        ListSwapMoveSelectorConfig config = new ListSwapMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        ListSwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        // Custom configuration
        config = new ListSwapMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getValueSelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();
    }

    @Test
    void assertEnableNearbyForKoptMoveSelectorConfig() {
        // Default configuration
        KOptListMoveSelectorConfig config = new KOptListMoveSelectorConfig();
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        KOptListMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getOriginSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();

        // Custom configuration
        config = new KOptListMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getValueSelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyConfig.getOriginSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass()).isNotNull();
    }

    @Test
    void assertEnableNearbyForUnionMoveSelectorConfig() {
        // Default configuration
        UnionMoveSelectorConfig config =
                new UnionMoveSelectorConfig(List.of(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig()));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        UnionMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getMoveSelectorList()).hasSize(4);

        ChangeMoveSelectorConfig nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        assertThat(nearbyChangeConfig.hasNearbySelectionConfig()).isFalse();

        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);
        assertThat(nearbyChangeConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        SwapMoveSelectorConfig nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(2);
        assertThat(nearbySwapConfig.hasNearbySelectionConfig()).isFalse();

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(3);
        assertThat(nearbySwapConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        // Custom configuration
        config =
                new UnionMoveSelectorConfig(List.of(
                        new ChangeMoveSelectorConfig().withEntitySelectorConfig(
                                new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC)),
                        new SwapMoveSelectorConfig()));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());

        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getMoveSelectorList()).hasSize(4);

        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        assertThat(nearbyChangeConfig.hasNearbySelectionConfig()).isFalse();

        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);
        assertThat(nearbyChangeConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getEntitySelectorConfig().getSelectionOrder()).isEqualTo(SelectionOrder.PROBABILISTIC);
        assertThat(nearbyChangeConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(2);
        assertThat(nearbySwapConfig.hasNearbySelectionConfig()).isFalse();

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(3);
        assertThat(nearbySwapConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSelectionOrder()).isNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();
    }

    @Test
    void assertEnableNearbyForMultipleUnionMoveSelectorConfig() {
        // Default configuration
        UnionMoveSelectorConfig unionChangeSelectorConfig =
                new UnionMoveSelectorConfig(List.of(new ChangeMoveSelectorConfig()));
        UnionMoveSelectorConfig unionSwapSelectorConfig =
                new UnionMoveSelectorConfig(List.of(new SwapMoveSelectorConfig(), new PillarSwapMoveSelectorConfig()));
        UnionMoveSelectorConfig config =
                new UnionMoveSelectorConfig(List.of(unionChangeSelectorConfig, unionSwapSelectorConfig));
        assertThat(config.hasNearbySelectionConfig()).isFalse();
        assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isTrue();
        UnionMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertThat(nearbyConfig).isNotNull();
        assertThat(nearbyConfig.hasNearbySelectionConfig()).isTrue();
        assertThat(nearbyConfig.getMoveSelectorList()).hasSize(2);

        UnionMoveSelectorConfig changeConfig = (UnionMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        ChangeMoveSelectorConfig nearbyChangeConfig = (ChangeMoveSelectorConfig) changeConfig.getMoveSelectorList().get(0);
        assertThat(nearbyChangeConfig.hasNearbySelectionConfig()).isFalse();

        nearbyChangeConfig = (ChangeMoveSelectorConfig) changeConfig.getMoveSelectorList().get(1);
        assertThat(nearbyChangeConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();

        UnionMoveSelectorConfig swapConfig = (UnionMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);
        assertThat(swapConfig.getMoveSelectorList()).hasSize(3);
        SwapMoveSelectorConfig nearbySwapConfig = (SwapMoveSelectorConfig) swapConfig.getMoveSelectorList().get(0);
        assertThat(nearbySwapConfig.hasNearbySelectionConfig()).isFalse();

        nearbySwapConfig = (SwapMoveSelectorConfig) swapConfig.getMoveSelectorList().get(1);
        assertThat(nearbySwapConfig.getEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig()).isNotNull();
        assertThat(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig()).isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig())
                .isNotNull();
        assertThat(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass())
                .isNotNull();
    }

    @Test
    void assertDisabledNearbyAutoConfiguration() {
        List<MoveSelectorConfig<?>> moveSelectorConfigList = List.of(
                new CartesianProductMoveSelectorConfig(),
                new MoveIteratorFactoryConfig(),
                new MoveListFactoryConfig(),
                new KOptMoveSelectorConfig(),
                new SubChainChangeMoveSelectorConfig(),
                new SubChainSwapMoveSelectorConfig(),
                new SubListChangeMoveSelectorConfig(),
                new SubListSwapMoveSelectorConfig(),
                new PillarChangeMoveSelectorConfig(),
                new PillarSwapMoveSelectorConfig());

        for (MoveSelectorConfig<?> config : moveSelectorConfigList) {
            assertThat(NearbyAutoConfigurationEnabled.class.isAssignableFrom(config.getClass())).isFalse();
            assertThat(config.hasNearbySelectionConfig()).isFalse();
        }
    }
}
