package ai.timefold.solver.core.impl.util;

import java.util.Objects;

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
        return o instanceof Pair<?, ?> other
                && Objects.equals(key, other.key)
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() { // Often used in hash-based collections; the JDK-generated default is too slow.
        var hash = 7;
        hash = 31 * hash + Objects.hashCode(key);
        hash = 31 * hash + Objects.hashCode(value);
        return hash;
    }

}
