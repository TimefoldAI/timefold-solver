package ai.timefold.solver.core.impl.util;

import java.util.Objects;

/**
 * A mutable key-value tuple.
 * Two instances {@link Object#equals(Object) are equal} if both values in the first instance
 * are equal to their counterpart in the other instance.
 *
 * @param <A>
 * @param <B>
 */
public final class MutablePair<A, B> {

    public static <A, B> MutablePair<A, B> of(A key, B value) {
        return new MutablePair<>(key, value);
    }

    private A key;
    private B value;

    private MutablePair(A key, B value) {
        this.key = key;
        this.value = value;
    }

    public A getKey() {
        return key;
    }

    public MutablePair<A, B> setKey(A key) {
        this.key = key;
        return this;
    }

    public B getValue() {
        return value;
    }

    public MutablePair<A, B> setValue(B value) {
        this.value = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MutablePair<?, ?> other) {
            return Objects.equals(key, other.key) && Objects.equals(value, other.value);
        }
        return false;
    }

    @Override
    public int hashCode() { // Not using Objects.hash(Object...) as that would create an array on the hot path.
        int result = Objects.hashCode(key);
        result = 31 * result + Objects.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}
