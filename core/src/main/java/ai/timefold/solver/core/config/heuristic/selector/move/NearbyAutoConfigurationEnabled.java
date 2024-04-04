package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

/**
 * For move selectors that support Nearby Selection autoconfiguration.
 */
public interface NearbyAutoConfigurationEnabled<Config_ extends MoveSelectorConfig<Config_>> {

    /**
     * @return new instance with the Nearby Selection settings properly configured
     */
    Config_ enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random);

}
