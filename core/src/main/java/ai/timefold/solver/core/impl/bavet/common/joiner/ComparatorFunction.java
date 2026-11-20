package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Comparator;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts a mapping function to return {@link ComparatorKey} values.
 *
 * @param <A> the mapped object type
 * @param <Property_> the property type
 */
@NullMarked
public record ComparatorFunction<A, Property_>(Function<A, Property_> delegate,
        Comparator<? super Property_> comparator) implements Function<A, ComparatorKey<Property_>> {

    @Override
    public ComparatorKey<Property_> apply(A a) {
        return new ComparatorKey<>(delegate.apply(a), comparator);
    }
}
