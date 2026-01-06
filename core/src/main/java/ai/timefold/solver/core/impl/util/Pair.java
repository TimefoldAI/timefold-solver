package ai.timefold.solver.core.impl.util;

/**
 * An immutable key-value tuple.
 * Two instances {@link Object#equals(Object) are equal} if both values in the first instance
 * are equal to their counterpart in the other instance.
 *
 * @param <Key_>
 * @param <Value_>
 */
public record Pair<Key_, Value_>(Key_ key, Value_ value) {

    @Override
    public boolean equals(Object o) {
        // We do not use Objects.equals(...) due to https://bugs.openjdk.org/browse/JDK-8015417.
        if (this == o) {
            return true;
        }
        return o instanceof Pair<?, ?> that &&
                ((key == that.key) || (key != null && key.equals(that.key))) &&
                ((value == that.value) || (value != null && value.equals(that.value)));
    }

    @Override
    public int hashCode() {
        // Often used in hash-based collections; the JDK-generated default is too slow.
        // We do not use Objects.hash(...) because it creates an array each time.
        // We do not use Objects.hashCode() due to https://bugs.openjdk.org/browse/JDK-8015417.
        var hash = 1;
        hash = 31 * hash + (key == null ? 0 : key.hashCode());
        return 31 * hash + (value == null ? 0 : value.hashCode());
        return hash;
    }

}
