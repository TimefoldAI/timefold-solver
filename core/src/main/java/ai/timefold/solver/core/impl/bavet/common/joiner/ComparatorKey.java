package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Comparator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A property value paired with the comparator that defines its order.
 * The comparison indexers require naturally {@link Comparable} keys;
 * this wrapper adapts any property type to that requirement without changing the indexers.
 *
 * @param <Property_> the property type
 */
@NullMarked
public record ComparatorKey<Property_>(@Nullable Property_ value, Comparator<? super Property_> comparator)
        implements
            Comparable<ComparatorKey<Property_>> {

    @Override
    public int compareTo(ComparatorKey<Property_> other) {
        return comparator.compare(value, other.value());
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        return o instanceof ComparatorKey<?> other && compareTo((ComparatorKey<Property_>) other) == 0;
    }

    @Override
    public int hashCode() {
        // Equality follows the comparator, which need not agree with Object.hashCode().
        return 0;
    }
}
