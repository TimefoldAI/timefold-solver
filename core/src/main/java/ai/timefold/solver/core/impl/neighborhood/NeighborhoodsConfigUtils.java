package ai.timefold.solver.core.impl.neighborhood;

import java.util.Map;
import java.util.function.Supplier;

import ai.timefold.solver.core.config.solver.NeighborhoodsConfig;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.preview.api.neighborhood.selection.MoveSelectionStrategy;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks and builders shared by the solver-level and the phase-level Neighborhoods API settings.
 */
@NullMarked
public final class NeighborhoodsConfigUtils {

    /**
     * @param neighborhoodsConfig the neighborhoods element of the level, null if absent
     * @param level "solver" or "phase"
     */
    public static void failWithoutNeighborhoods(@Nullable Object neighborhoodsConfig, String level,
            boolean neighborhoodsEnabled) {
        if (neighborhoodsConfig != null && !neighborhoodsEnabled) {
            throw new UnsupportedOperationException("""
                    The %s-level neighborhoods element can only be used if the %s preview feature is enabled.
                    Maybe add <enablePreviewFeature>%s</enablePreviewFeature> to your solver configuration file?"""
                    .formatted(level, PreviewFeature.NEIGHBORHOODS, PreviewFeature.NEIGHBORHOODS));
        }
    }

    public static void failPropertiesWithoutClass(@Nullable Class<?> strategyClass,
            @Nullable Map<String, String> strategyProperties, String level) {
        if (strategyClass == null && strategyProperties != null) {
            throw new IllegalArgumentException("""
                    The %s-level moveSelectionStrategyCustomProperties (%s) need a moveSelectionStrategyClass at the same level.
                    Maybe set the moveSelectionStrategyClass of the %s too."""
                    .formatted(level, strategyProperties, level));
        }
    }

    /**
     * Fails fast on a wrong class or an unknown custom property.
     *
     * @return gives a new strategy, with its custom properties applied, at each call;
     *         later changes to strategyProperties do not reach it
     */
    public static <Solution_> Supplier<MoveSelectionStrategy<Solution_>> buildStrategyFactory(
            Class<? extends MoveSelectionStrategy<Solution_>> strategyClass,
            @Nullable Map<String, String> strategyProperties) {
        if (!MoveSelectionStrategy.class.isAssignableFrom(strategyClass)) {
            throw new IllegalArgumentException("The moveSelectionStrategyClass (%s) does not implement %s."
                    .formatted(strategyClass, MoveSelectionStrategy.class.getSimpleName()));
        }
        ConfigUtils.assertCustomPropertiesSettable(strategyClass, "moveSelectionStrategyClass", strategyProperties,
                "moveSelectionStrategyCustomProperties");
        var propertiesCopy = strategyProperties == null ? null : Map.copyOf(strategyProperties);
        return () -> {
            var strategy = ConfigUtils.newInstance(NeighborhoodsConfig.class::getSimpleName, "moveSelectionStrategyClass",
                    strategyClass);
            ConfigUtils.applyCustomProperties(strategy, "moveSelectionStrategyClass", propertiesCopy,
                    "moveSelectionStrategyCustomProperties");
            return strategy;
        };
    }

    private NeighborhoodsConfigUtils() {
    }

}
