package ai.timefold.solver.core.config.heuristic.selector.move;

import java.util.Random;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;

/**
 * General superclass for move selectors that support Nearby Selection autoconfiguration.
 */
public abstract class NearbyAutoConfigurationMoveSelectorConfig<Config_ extends MoveSelectorConfig<Config_>>
        extends MoveSelectorConfig<Config_> {

    /**
     * Enables the Nearby Selection autoconfiguration.
     *
     * @return new instance with the Nearby Selection settings properly configured
     */
    public abstract Config_ enableNearbySelection(Class<? extends NearbyDistanceMeter<?, ?>> distanceMeter, Random random);

    protected static String addRandomSuffix(String name, Random random) {
        StringBuilder value = new StringBuilder(name);
        value.append("-");
        random.ints(97, 122) // ['a', 'z']
                .limit(4) // 4 letters
                .forEach(value::appendCodePoint);
        return value.toString();
    }
}
