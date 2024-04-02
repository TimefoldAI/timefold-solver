package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.SwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.chained.TailChainSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

public final class NearbyUtil {

    public static ChangeMoveSelectorConfig enable(ChangeMoveSelectorConfig changeMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        ChangeMoveSelectorConfig nearbyConfig = changeMoveSelectorConfig.copyConfig();
        EntitySelectorConfig entityConfig = nearbyConfig.getEntitySelectorConfig();
        if (entityConfig == null) {
            entityConfig = new EntitySelectorConfig();
        }
        String entitySelectorId = addRandomSuffix("entitySelector", random);
        entityConfig.withId(entitySelectorId);
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        valueConfig.withNearbySelectionConfig(
                new NearbySelectionConfig()
                        .withOriginEntitySelectorConfig(new EntitySelectorConfig()
                                .withMimicSelectorRef(entitySelectorId))
                        .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    public static SwapMoveSelectorConfig enable(SwapMoveSelectorConfig swapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        SwapMoveSelectorConfig nearbyConfig = swapMoveSelectorConfig.copyConfig();
        EntitySelectorConfig entityConfig = nearbyConfig.getEntitySelectorConfig();
        if (entityConfig == null) {
            entityConfig = new EntitySelectorConfig();
        }
        String entitySelectorId = addRandomSuffix("entitySelector", random);
        entityConfig.withId(entitySelectorId);
        EntitySelectorConfig secondaryConfig = nearbyConfig.getSecondaryEntitySelectorConfig();
        if (secondaryConfig == null) {
            secondaryConfig = new EntitySelectorConfig();
        }
        secondaryConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginEntitySelectorConfig(new EntitySelectorConfig()
                        .withMimicSelectorRef(entitySelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withSecondaryEntitySelectorConfig(secondaryConfig);
        return nearbyConfig;
    }

    public static TailChainSwapMoveSelectorConfig enable(TailChainSwapMoveSelectorConfig tailChainSwapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        TailChainSwapMoveSelectorConfig nearbyConfig = tailChainSwapMoveSelectorConfig.copyConfig();
        EntitySelectorConfig entityConfig = nearbyConfig.getEntitySelectorConfig();
        if (entityConfig == null) {
            entityConfig = new EntitySelectorConfig();
        }
        String entitySelectorId = addRandomSuffix("entitySelector", random);
        entityConfig.withId(entitySelectorId);
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        valueConfig.withNearbySelectionConfig(
                new NearbySelectionConfig()
                        .withOriginEntitySelectorConfig(new EntitySelectorConfig()
                                .withMimicSelectorRef(entitySelectorId))
                        .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    public static ListChangeMoveSelectorConfig enable(ListChangeMoveSelectorConfig listChangeMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter,
            Random random, boolean onlyUnassigned) {
        ListChangeMoveSelectorConfig nearbyConfig = listChangeMoveSelectorConfig.copyConfig();
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        String valueSelectorId = addRandomSuffix("valueSelector", random);
        if (onlyUnassigned) {
            throw new UnsupportedOperationException();
        }
        valueConfig.withId(valueSelectorId);
        DestinationSelectorConfig destinationConfig = nearbyConfig.getDestinationSelectorConfig();
        if (destinationConfig == null) {
            destinationConfig = new DestinationSelectorConfig();
        }
        destinationConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(valueSelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withDestinationSelectorConfig(destinationConfig);
        return nearbyConfig;
    }

    public static ListSwapMoveSelectorConfig enable(ListSwapMoveSelectorConfig listSwapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        ListSwapMoveSelectorConfig nearbyConfig = listSwapMoveSelectorConfig.copyConfig();
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        String valueSelectorId = addRandomSuffix("valueSelector", random);
        valueConfig.withId(valueSelectorId);
        ValueSelectorConfig secondaryConfig = nearbyConfig.getSecondaryValueSelectorConfig();
        if (secondaryConfig == null) {
            secondaryConfig = new ValueSelectorConfig();
        }
        secondaryConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(valueSelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withSecondaryValueSelectorConfig(secondaryConfig);
        return nearbyConfig;
    }

    public static KOptListMoveSelectorConfig enable(KOptListMoveSelectorConfig kOptListMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        KOptListMoveSelectorConfig nearbyConfig = kOptListMoveSelectorConfig.copyConfig();
        ValueSelectorConfig originConfig = nearbyConfig.getOriginSelectorConfig();
        if (originConfig == null) {
            originConfig = new ValueSelectorConfig();
        }
        String valueSelectorId = addRandomSuffix("valueSelector", random);
        originConfig.withId(valueSelectorId);
        ValueSelectorConfig valueConfig = nearbyConfig.getValueSelectorConfig();
        if (valueConfig == null) {
            valueConfig = new ValueSelectorConfig();
        }
        valueConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(valueSelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withOriginSelectorConfig(originConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    private static String addRandomSuffix(String name, Random random) {
        StringBuilder value = new StringBuilder(name);
        value.append("-");
        random.ints(97, 122) // ['a', 'z']
                .limit(4) // 4 letters
                .forEach(value::appendCodePoint);
        return value.toString();
    }

    private NearbyUtil() {
        // No instances.
    }

}
