package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import java.util.TreeMap;
import java.util.TreeSet;

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
record ComparableValue<Value_, Point_ extends Comparable<Point_>>(Value_ value, Point_ index)
        implements
            Comparable<ComparableValue<Value_, Point_>> {

    @Override
    public int compareTo(ComparableValue<Value_, Point_> other) {
        if (this == other) {
            return 0;
        }
        Point_ point1 = this.index;
        Point_ point2 = other.index;
        if (point1 != point2) {
            int comparison = point1.compareTo(point2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return compareWithIdentityHashCode(this.value, other.value);
    }

    private int compareWithIdentityHashCode(Value_ o1, Value_ o2) {
        if (o1 == o2) {
            return 0;
        }
        // Identity Hashcode for duplicate protection; we must always include duplicates.
        // Ex: two different games on the same time slot
        int identityHashCode1 = System.identityHashCode(o1);
        int identityHashCode2 = System.identityHashCode(o2);
        return Integer.compare(identityHashCode1, identityHashCode2);
    }

}
