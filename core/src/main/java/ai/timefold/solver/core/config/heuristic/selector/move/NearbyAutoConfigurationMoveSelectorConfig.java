package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

/**
 * General superclass for move selectors that support Nearby Selection autoconfiguration.
 */
public abstract class NearbyAutoConfigurationMoveSelectorConfig<Config_ extends MoveSelectorConfig<Config_>>
        extends MoveSelectorConfig<Config_> {

    /**
     * @return new instance with the Nearby Selection settings properly configured
     */
    public abstract Config_ enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random);

}
