package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Arrays;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * Often replaced by a specialization such as {@link Pair}, {@link Triple}, ...
 * Overrides {@link Object#equals(Object)} and {@link Object#hashCode()} as it references an external object.
 */
record IndexerKey(Object... properties) {

    @Override
    public boolean equals(Object o) {
        return o instanceof IndexerKey other && Arrays.deepEquals(properties, other.properties);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(properties);
    }

    @Override
    public String toString() {
        return Arrays.toString(properties);
    }
}
