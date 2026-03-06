package ai.timefold.solver.core.api.domain.specification;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Fluent builder for a list {@link VariableSpecification}.
 *
 * @param <S> the solution type
 * @param <E> the entity type
 * @param <V> the list element type
 */
public interface ListVariableSpecificationBuilder<S, E, V> {

    ListVariableSpecificationBuilder<S, E, V> accessors(Function<E, List<V>> getter, BiConsumer<E, List<V>> setter);

    ListVariableSpecificationBuilder<S, E, V> valueRange(String... refs);

    ListVariableSpecificationBuilder<S, E, V> allowsUnassignedValues(boolean allows);

    ListVariableSpecificationBuilder<S, E, V> strengthComparator(Comparator<V> comparator);
}
