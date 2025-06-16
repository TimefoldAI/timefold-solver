package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

import org.jspecify.annotations.NonNull;

/**
 * For move selectors that support Nearby Selection autoconfiguration.
 */
public interface NearbyAutoConfigurationEnabled<Config_ extends MoveSelectorConfig<Config_>> {

    /**
     * @return true if it can enable the nearby setting for the given move configuration; otherwise, it returns false.
     */
    boolean canEnableNearbyInMixedModels();

    /**
     * @return new instance with the Nearby Selection settings properly configured
     */
    @NonNull
    Config_ enableNearbySelection(@NonNull Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, @NonNull Random random);

}
