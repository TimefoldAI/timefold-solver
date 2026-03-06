package ai.timefold.solver.core.api.domain.specification;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Fluent builder for a basic (non-list) {@link VariableSpecification}.
 *
 * @param <S> the solution type
 * @param <E> the entity type
 * @param <V> the variable value type
 */
public interface VariableSpecificationBuilder<S, E, V> {

    VariableSpecificationBuilder<S, E, V> accessors(Function<E, V> getter, BiConsumer<E, V> setter);

    VariableSpecificationBuilder<S, E, V> valueRange(String... refs);

    VariableSpecificationBuilder<S, E, V> allowsUnassigned(boolean allows);

    VariableSpecificationBuilder<S, E, V> strengthComparator(Comparator<V> comparator);
}
