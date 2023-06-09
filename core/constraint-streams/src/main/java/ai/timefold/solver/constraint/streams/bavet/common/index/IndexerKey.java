package ai.timefold.solver.constraint.streams.bavet.common.index;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Triple;

/**
 * Often replaced by a specialization such as {@link Pair}, {@link Triple}, ...
 */
final class IndexerKey {

    private final IndexProperties indexProperties;
    private final int fromInclusive;
    private final int toExclusive;

    public IndexerKey(IndexProperties indexProperties, int fromInclusive, int toExclusive) {
        this.indexProperties = indexProperties;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
    }

    @Override
    public int hashCode() {
        if (indexProperties == null) {
            return 0;
        }
        int result = 1;
        for (int i = fromInclusive; i < toExclusive; i++) {
            Object element = indexProperties.toKey(i);
            result = 31 * result + (element == null ? 0 : element.hashCode());
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IndexerKey)) {
            return false;
        }
        IndexerKey other = (IndexerKey) o;
        for (int i = fromInclusive; i < toExclusive; i++) {
            Object a = indexProperties.toKey(i);
            Object b = other.indexProperties.toKey(i);
            if (!Objects.equals(a, b)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "IndexerKey " + IntStream.range(fromInclusive, toExclusive)
                .mapToObj(indexProperties::toKey)
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));

    }
}
