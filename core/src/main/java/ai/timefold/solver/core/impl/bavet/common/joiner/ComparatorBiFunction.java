package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Comparator;
import java.util.function.BiFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts a bi mapping function to return {@link ComparatorKey} values.
 *
 * @param <A> the first mapped object type
 * @param <B> the second mapped object type
 * @param <Property_> the property type
 */
@NullMarked
public record ComparatorBiFunction<A, B, Property_>(BiFunction<A, B, Property_> delegate,
        Comparator<? super Property_> comparator) implements BiFunction<A, B, ComparatorKey<Property_>> {

    @Override
    public ComparatorKey<Property_> apply(A a, B b) {
        return new ComparatorKey<>(delegate.apply(a, b), comparator);
    }
}
