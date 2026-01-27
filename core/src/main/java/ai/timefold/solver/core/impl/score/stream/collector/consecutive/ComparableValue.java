package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jspecify.annotations.NullMarked;

/**
 * Each {@link #value()} is associated with a point ({@link #index()}) on the number line.
 * Comparisons are made using the points on the number line, not the actual values.
 *
 * <p>
 * {@link #equals(Object)} and {@link #hashCode()} of this class is not a concern,
 * as it is only used in a {@link TreeSet} or {@link TreeMap}.
 * No two values {@link #compareTo(ComparableValue) compare} equal unless they are the same object,
 * even if they are in the same position on the number line.
 *
 * @param value the value to be put on the number line
 * @param index position of the value on the number line
 * @param <Value_> generic type of the value
 * @param <Point_> generic type of the point on the number line
 */
@NullMarked
record ComparableValue<Value_, Point_ extends Comparable<Point_>>(Value_ value, Point_ index)
        implements
            Comparable<ComparableValue<Value_, Point_>> {

    @Override
    public int compareTo(ComparableValue<Value_, Point_> other) {
        if (this == other) {
            return 0;
        }
        var out = index.compareTo(other.index);
        if (out == 0) {
            return compareWithIdentityHashCode(value, other.value);
        }
        return out;
    }

    private int compareWithIdentityHashCode(Value_ o1, Value_ o2) {
        if (o1 == o2) {
            return 0;
        }
        // Identity Hashcode for duplicate protection; we must always include duplicates.
        // Ex: two different games on the same time slot
        var identityHashCode1 = System.identityHashCode(o1);
        var identityHashCode2 = System.identityHashCode(o2);
        return Integer.compare(identityHashCode1, identityHashCode2);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ComparableValue<?, ?> that)) {
            return false;
        }
        return value == that.value && Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(value), index);
    }
}
