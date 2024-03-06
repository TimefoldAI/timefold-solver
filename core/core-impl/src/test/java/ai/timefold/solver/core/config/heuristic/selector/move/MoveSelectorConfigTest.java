package ai.timefold.solver.core.config.heuristic.selector.move;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        ChangeMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new ChangeMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getEntitySelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForSwapMoveSelectorConfig() {
        // Default configuration
        SwapMoveSelectorConfig config = new SwapMoveSelectorConfig();
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        SwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig());
        assertNotNull(
                nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new SwapMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getEntitySelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig());
        assertNotNull(
                nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForTailChainMoveSelectorConfig() {
        // Default configuration
        TailChainSwapMoveSelectorConfig config = new TailChainSwapMoveSelectorConfig();
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        TailChainSwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new TailChainSwapMoveSelectorConfig();
        config.withEntitySelectorConfig(new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getEntitySelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getEntitySelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForListChangeMoveSelectorConfig() {
        // Default configuration
        ListChangeMoveSelectorConfig config = new ListChangeMoveSelectorConfig();
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        ListChangeMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getOriginValueSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new ListChangeMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getValueSelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getOriginValueSelectorConfig());
        assertNotNull(nearbyConfig.getDestinationSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForListSwapMoveSelectorConfig() {
        // Default configuration
        ListSwapMoveSelectorConfig config = new ListSwapMoveSelectorConfig();
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        ListSwapMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new ListSwapMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getValueSelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getSecondaryValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForKoptMoveSelectorConfig() {
        // Default configuration
        KOptListMoveSelectorConfig config = new KOptListMoveSelectorConfig();
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        KOptListMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertNotNull(nearbyConfig.getOriginSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config = new KOptListMoveSelectorConfig();
        config.withValueSelectorConfig(new ValueSelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyConfig.getValueSelectorConfig().getSelectionOrder());
        assertNotNull(nearbyConfig.getOriginSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForUnionMoveSelectorConfig() {
        // Default configuration
        UnionMoveSelectorConfig config =
                new UnionMoveSelectorConfig(List.of(new ChangeMoveSelectorConfig(), new SwapMoveSelectorConfig()));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        UnionMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(4, nearbyConfig.getMoveSelectorList().size());

        ChangeMoveSelectorConfig nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        assertFalse(nearbyChangeConfig.hasNearbySelectionConfig());

        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);
        assertNotNull(nearbyChangeConfig.getEntitySelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        SwapMoveSelectorConfig nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(2);
        assertFalse(nearbySwapConfig.hasNearbySelectionConfig());

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(3);
        assertNotNull(nearbySwapConfig.getEntitySelectorConfig());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        // Custom configuration
        config =
                new UnionMoveSelectorConfig(List.of(
                        new ChangeMoveSelectorConfig().withEntitySelectorConfig(
                                new EntitySelectorConfig().withSelectionOrder(SelectionOrder.PROBABILISTIC)),
                        new SwapMoveSelectorConfig()));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());

        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(4, nearbyConfig.getMoveSelectorList().size());

        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        assertFalse(nearbyChangeConfig.hasNearbySelectionConfig());


        nearbyChangeConfig = (ChangeMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);
        assertNotNull(nearbyChangeConfig.getEntitySelectorConfig());
        assertEquals(SelectionOrder.PROBABILISTIC, nearbyChangeConfig.getEntitySelectorConfig().getSelectionOrder());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(2);
        assertFalse(nearbySwapConfig.hasNearbySelectionConfig());

        nearbySwapConfig = (SwapMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(3);
        assertNotNull(nearbySwapConfig.getEntitySelectorConfig());
        assertNull(nearbySwapConfig.getSelectionOrder());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
    }

    @Test
    void assertEnableNearbyForMultipleUnionMoveSelectorConfig() {
        // Default configuration
        UnionMoveSelectorConfig unionChangeSelectorConfig =
                new UnionMoveSelectorConfig(List.of(new ChangeMoveSelectorConfig()));
        UnionMoveSelectorConfig unionSwapSelectorConfig =
                new UnionMoveSelectorConfig(List.of(new SwapMoveSelectorConfig()));
        UnionMoveSelectorConfig config =
                new UnionMoveSelectorConfig(List.of(unionChangeSelectorConfig, unionSwapSelectorConfig));
        assertFalse(config.hasNearbySelectionConfig());
        assertTrue(config.acceptNearbySelectionAutoConfiguration());
        UnionMoveSelectorConfig nearbyConfig = config.enableNearbySelection(TestDistanceMeter.class, new Random());
        assertTrue(nearbyConfig.hasNearbySelectionConfig());
        assertNotNull(nearbyConfig);
        assertEquals(2, nearbyConfig.getMoveSelectorList().size());

        UnionMoveSelectorConfig changeConfig = (UnionMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(0);
        ChangeMoveSelectorConfig nearbyChangeConfig = (ChangeMoveSelectorConfig) changeConfig.getMoveSelectorList().get(0);
        assertFalse(nearbyChangeConfig.hasNearbySelectionConfig());

        nearbyChangeConfig = (ChangeMoveSelectorConfig) changeConfig.getMoveSelectorList().get(1);
        assertNotNull(nearbyChangeConfig.getEntitySelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(nearbyChangeConfig.getValueSelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());

        UnionMoveSelectorConfig swapConfig = (UnionMoveSelectorConfig) nearbyConfig.getMoveSelectorList().get(1);

        SwapMoveSelectorConfig nearbySwapConfig = (SwapMoveSelectorConfig) swapConfig.getMoveSelectorList().get(0);
        assertFalse(nearbySwapConfig.hasNearbySelectionConfig());

        nearbySwapConfig = (SwapMoveSelectorConfig) swapConfig.getMoveSelectorList().get(1);
        assertNotNull(nearbySwapConfig.getEntitySelectorConfig());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig());
        assertNotNull(nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getOriginEntitySelectorConfig());
        assertNotNull(
                nearbySwapConfig.getSecondaryEntitySelectorConfig().getNearbySelectionConfig().getNearbyDistanceMeterClass());
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
            assertFalse(config.acceptNearbySelectionAutoConfiguration());
            assertFalse(config.hasNearbySelectionConfig());
        }
    }
}
