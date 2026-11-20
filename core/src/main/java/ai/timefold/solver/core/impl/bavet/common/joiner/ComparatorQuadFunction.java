package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Comparator;

import ai.timefold.solver.core.api.function.QuadFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts a quad mapping function to return {@link ComparatorKey} values.
 *
 * @param <A> the first mapped object type
 * @param <B> the second mapped object type
 * @param <C> the third mapped object type
 * @param <D> the fourth mapped object type
 * @param <Property_> the property type
 */
@NullMarked
public record ComparatorQuadFunction<A, B, C, D, Property_>(QuadFunction<A, B, C, D, Property_> delegate,
        Comparator<? super Property_> comparator) implements QuadFunction<A, B, C, D, ComparatorKey<Property_>> {

    @Override
    public ComparatorKey<Property_> apply(A a, B b, C c, D d) {
        return new ComparatorKey<>(delegate.apply(a, b, c, d), comparator);
    }
}
