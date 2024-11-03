package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.jspecify.annotations.NonNull;

/**
 * For move selectors that support Nearby Selection autoconfiguration.
 */
public interface NearbyAutoConfigurationEnabled<Config_ extends MoveSelectorConfig<Config_>> {

    /**
     * @return new instance with the Nearby Selection settings properly configured
     */
    @NonNull
    // TODO: distanceMeter and random @NonNull?
    Config_ enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random);

}
