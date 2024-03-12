package ai.timefold.solver.core.config.heuristic.selector;

import ai.timefold.solver.core.config.AbstractConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;

/**
 * General superclass for {@link MoveSelectorConfig}, {@link EntitySelectorConfig} and {@link ValueSelectorConfig}.
 */
public abstract class SelectorConfig<Config_ extends SelectorConfig<Config_>> extends AbstractConfig<Config_> {

    /**
     * Verifies if the current configuration has any Nearby Selection settings.
     */
    public abstract boolean hasNearbySelectionConfig();
}
