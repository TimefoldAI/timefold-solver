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

    public Pair<Key_, Value_> newIfDifferent(Key_ newA, Value_ newB) {
        return Objects.equals(key, newA) && Objects.equals(value, newB) ? this : new Pair<>(newA, newB);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pair<?, ?> other &&
                Objects.equals(key, other.key) &&
                Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        var hash = 1;
        hash = 31 * hash + Objects.hashCode(key);
        hash = 31 * hash + Objects.hashCode(value);
        return hash;
    }

}
