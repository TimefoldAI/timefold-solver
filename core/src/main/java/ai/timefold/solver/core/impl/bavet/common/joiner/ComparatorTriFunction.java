package ai.timefold.solver.core.impl.bavet.common.joiner;

import java.util.Comparator;

import ai.timefold.solver.core.api.function.TriFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts a tri mapping function to return {@link ComparatorKey} values.
 *
 * @param <A> the first mapped object type
 * @param <B> the second mapped object type
 * @param <C> the third mapped object type
 * @param <Property_> the property type
 */
@NullMarked
public record ComparatorTriFunction<A, B, C, Property_>(TriFunction<A, B, C, Property_> delegate,
        Comparator<? super Property_> comparator) implements TriFunction<A, B, C, ComparatorKey<Property_>> {

    @Override
    public ComparatorKey<Property_> apply(A a, B b, C c) {
        return new ComparatorKey<>(delegate.apply(a, b, c), comparator);
    }
}
