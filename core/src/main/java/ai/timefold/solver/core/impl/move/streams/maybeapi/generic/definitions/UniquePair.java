package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.definitions;

import java.util.Objects;

/**
 * A pair of two entities where (a, b) is considered equal to (b, a).
 */
record UniquePair<Entity_>(Entity_ first, Entity_ second) {

    @Override
    public boolean equals(Object o) {
        return o instanceof UniquePair<?> other &&
                ((first == other.first && second == other.second) || (first == other.second && second == other.first));
    }

    @Override
    public int hashCode() {
        var firstHash = Objects.hashCode(first);
        var secondHash = Objects.hashCode(second);
        // We always include both hashes, so that the order of first and second does not matter.
        // We compute in long to minimize intermediate overflows.
        var longHash = (31L * firstHash * secondHash) + firstHash + secondHash;
        return Long.hashCode(longHash);
    }
}
