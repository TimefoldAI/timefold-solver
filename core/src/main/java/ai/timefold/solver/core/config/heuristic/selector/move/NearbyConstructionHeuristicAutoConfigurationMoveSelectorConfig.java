package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

/**
 * General superclass for move selectors that support Nearby Selection autoconfiguration in construction heuristics.
 */
public abstract class NearbyConstructionHeuristicAutoConfigurationMoveSelectorConfig<Config_ extends MoveSelectorConfig<Config_>>
        extends NearbyAutoConfigurationMoveSelectorConfig<Config_> {

    /**
     * @return new instance with the Nearby Selection settings properly configured
     */
    public abstract Config_ enableNearbySelectionForConstructionHeuristic(
            Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random, String recordingSelectorId);

}
