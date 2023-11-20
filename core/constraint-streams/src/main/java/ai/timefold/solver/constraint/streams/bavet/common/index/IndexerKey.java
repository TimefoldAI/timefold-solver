package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * Often replaced by a specialization such as {@link Pair}, {@link Triple}, ...
 * Overrides {@link Object#equals(Object)} and {@link Object#hashCode()} as it references external object.
 */
record IndexerKey(IndexProperties indexProperties, int fromInclusive, int toExclusive) {

    @Override
    public boolean equals(Object o) {
        if (o instanceof IndexerKey other) {
            for (var i = fromInclusive; i < toExclusive; i++) {
                var a = indexProperties.toKey(i);
                var b = other.indexProperties.toKey(i);
                if (!Objects.equals(a, b)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (indexProperties == null) {
            return 0;
        }
        var result = 1;
        for (var i = fromInclusive; i < toExclusive; i++) {
            var element = indexProperties.toKey(i);
            result = 31 * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    @Override
    public String toString() {
        return "IndexerKey " + IntStream.range(fromInclusive, toExclusive)
                .mapToObj(indexProperties::toKey)
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
