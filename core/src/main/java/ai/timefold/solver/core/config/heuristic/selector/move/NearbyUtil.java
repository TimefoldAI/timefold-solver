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
        var nearbyConfig = changeMoveSelectorConfig.copyConfig();
        var entityConfig = configureEntitySelector(nearbyConfig.getEntitySelectorConfig(), random);
        var valueConfig = configureValueSelector(nearbyConfig.getValueSelectorConfig(), entityConfig, distanceMeter);
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    private static EntitySelectorConfig configureEntitySelector(EntitySelectorConfig entitySelectorConfig, Random random) {
        if (entitySelectorConfig == null) {
            entitySelectorConfig = new EntitySelectorConfig();
        }
        var entitySelectorId = addRandomSuffix("entitySelector", random);
        entitySelectorConfig.withId(entitySelectorId);
        return entitySelectorConfig;
    }

    private static ValueSelectorConfig configureValueSelector(ValueSelectorConfig valueSelectorConfig,
            EntitySelectorConfig entitySelectorConfig, Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter) {
        if (valueSelectorConfig == null) {
            valueSelectorConfig = new ValueSelectorConfig();
        }
        return valueSelectorConfig.withNearbySelectionConfig(
                new NearbySelectionConfig()
                        .withOriginEntitySelectorConfig(new EntitySelectorConfig()
                                .withMimicSelectorRef(entitySelectorConfig.getId()))
                        .withNearbyDistanceMeterClass(distanceMeter));
    }

    public static SwapMoveSelectorConfig enable(SwapMoveSelectorConfig swapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        var nearbyConfig = swapMoveSelectorConfig.copyConfig();
        var entityConfig = configureEntitySelector(nearbyConfig.getEntitySelectorConfig(), random);
        var secondaryConfig = nearbyConfig.getSecondaryEntitySelectorConfig();
        if (secondaryConfig == null) {
            secondaryConfig = new EntitySelectorConfig();
        }
        secondaryConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginEntitySelectorConfig(new EntitySelectorConfig()
                        .withMimicSelectorRef(entityConfig.getId()))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withSecondaryEntitySelectorConfig(secondaryConfig);
        return nearbyConfig;
    }

    public static TailChainSwapMoveSelectorConfig enable(TailChainSwapMoveSelectorConfig tailChainSwapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        var nearbyConfig = tailChainSwapMoveSelectorConfig.copyConfig();
        var entityConfig = configureEntitySelector(nearbyConfig.getEntitySelectorConfig(), random);
        var valueConfig = configureValueSelector(nearbyConfig.getValueSelectorConfig(), entityConfig, distanceMeter);
        nearbyConfig.withEntitySelectorConfig(entityConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    public static ListChangeMoveSelectorConfig enable(ListChangeMoveSelectorConfig listChangeMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        var nearbyConfig = listChangeMoveSelectorConfig.copyConfig();
        var valueConfig = configureValueSelector(nearbyConfig.getValueSelectorConfig(), random);
        var destinationConfig = nearbyConfig.getDestinationSelectorConfig();
        if (destinationConfig == null) {
            destinationConfig = new DestinationSelectorConfig();
        }
        destinationConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(valueConfig.getId()))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withDestinationSelectorConfig(destinationConfig);
        return nearbyConfig;
    }

    public static ListChangeMoveSelectorConfig enable(ListChangeMoveSelectorConfig listChangeMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, String recordingSelectorId) {
        var nearbyConfig = listChangeMoveSelectorConfig.copyConfig();
        var valueConfig = new ValueSelectorConfig()
                .withMimicSelectorRef(recordingSelectorId);
        var destinationConfig = nearbyConfig.getDestinationSelectorConfig();
        if (destinationConfig == null) {
            destinationConfig = new DestinationSelectorConfig();
        }
        destinationConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(recordingSelectorId))
                .withNearbyDistanceMeterClass(distanceMeter));
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withDestinationSelectorConfig(destinationConfig);
        return nearbyConfig;
    }

    private static ValueSelectorConfig configureValueSelector(ValueSelectorConfig valueSelectorConfig, Random random) {
        if (valueSelectorConfig == null) {
            valueSelectorConfig = new ValueSelectorConfig();
        }
        var valueSelectorId = addRandomSuffix("valueSelector", random);
        valueSelectorConfig.withId(valueSelectorId);
        return valueSelectorConfig;
    }

    public static ListSwapMoveSelectorConfig enable(ListSwapMoveSelectorConfig listSwapMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        var nearbyConfig = listSwapMoveSelectorConfig.copyConfig();
        var valueConfig = configureValueSelector(nearbyConfig.getValueSelectorConfig(), random);
        var secondaryConfig =
                configureSecondaryValueSelector(nearbyConfig.getSecondaryValueSelectorConfig(), valueConfig, distanceMeter);
        nearbyConfig.withValueSelectorConfig(valueConfig)
                .withSecondaryValueSelectorConfig(secondaryConfig);
        return nearbyConfig;
    }

    private static ValueSelectorConfig configureSecondaryValueSelector(ValueSelectorConfig secondaryValueSelectorConfig,
            ValueSelectorConfig primaryValueSelectorConfig, Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter) {
        if (secondaryValueSelectorConfig == null) {
            secondaryValueSelectorConfig = new ValueSelectorConfig();
        }
        secondaryValueSelectorConfig.withNearbySelectionConfig(new NearbySelectionConfig()
                .withOriginValueSelectorConfig(new ValueSelectorConfig()
                        .withMimicSelectorRef(primaryValueSelectorConfig.getId()))
                .withNearbyDistanceMeterClass(distanceMeter));
        return secondaryValueSelectorConfig;
    }

    public static KOptListMoveSelectorConfig enable(KOptListMoveSelectorConfig kOptListMoveSelectorConfig,
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random) {
        var nearbyConfig = kOptListMoveSelectorConfig.copyConfig();
        var originConfig = configureValueSelector(nearbyConfig.getOriginSelectorConfig(), random);
        var valueConfig = configureSecondaryValueSelector(nearbyConfig.getValueSelectorConfig(), originConfig, distanceMeter);
        nearbyConfig.withOriginSelectorConfig(originConfig)
                .withValueSelectorConfig(valueConfig);
        return nearbyConfig;
    }

    private static String addRandomSuffix(String name, Random random) {
        var value = new StringBuilder(name);
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
