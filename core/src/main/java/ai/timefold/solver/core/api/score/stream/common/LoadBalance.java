package ai.timefold.solver.core.api.score.stream.common;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;

/**
 * Represents the result of a load-balancing constraint collector.
 *
 * @see ConstraintCollectors#loadBalance(Function) More information about load-balancing constraint collectors.
 */
public interface LoadBalance {

    /**
     * Raw value of the unfairness measure; the higher the value, the more unbalanced the load among the keys.
     * This value is dimension-less; it only serves to compare different solutions of the same dataset to each other.
     * Once the constraint implementation changes, or when the number of entities/values in the dataset changes,
     * the unfairness value is no longer comparable with values from the original dataset.
     * <p>
     * You should not rely on the exact value of unfairness,
     * or on the absolute differences between two unfairness values.
     * We do not guarantee the long-term stability of the underlying formula;
     * we only guarantee the properties of the value as described above.
     *
     * @return >= 0.
     */
    BigDecimal unfairness();

    /**
     * Represents the individual loads for each key.
     * The key is the entity or value being load-balanced, the value is the load of that entity or value.
     * If a key is not present, it was never supplied to the collector.
     * <p>
     * If you intend to use the map in its entirety as opposed to one-time extracting individual values for keys,
     * you must create a defensive copy.
     * Later CS calculations will modify the underlying storage, resulting in changes to this map.
     * <p>
     * This method is intended for use in constraint justifications and indictments.
     * Processing the (possibly very large) map sequentially within constraints is not recommended,
     * as it will negatively impact the scalability of incremental score calculation.
     *
     * @return never null; unmodifiable.
     */
    Map<Object, Long> loads();

}
