package ai.timefold.solver.core.api.domain.specification;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Fluent builder for an {@link EntitySpecification}.
 *
 * @param <S> the solution type
 * @param <E> the entity type
 */
public interface EntitySpecificationBuilder<S, E> {

    <T extends Comparable<T>> EntitySpecificationBuilder<S, E> planningId(Function<E, T> getter);

    <T extends Comparable<T>> EntitySpecificationBuilder<S, E> planningId(Function<E, T> getter,
            BiConsumer<E, T> setter);

    EntitySpecificationBuilder<S, E> difficultyComparator(Comparator<E> comparator);

    EntitySpecificationBuilder<S, E> pinned(Predicate<E> isPinned);

    EntitySpecificationBuilder<S, E> pinToIndex(ToIntFunction<E> pinToIndex);

    EntitySpecificationBuilder<S, E> valueRange(String id, Function<E, ?> getter);

    <V> EntitySpecificationBuilder<S, E> variable(String name, Class<V> valueType,
            Consumer<VariableSpecificationBuilder<S, E, V>> config);

    <V> EntitySpecificationBuilder<S, E> listVariable(String name, Class<V> elementType,
            Consumer<ListVariableSpecificationBuilder<S, E, V>> config);

    <V> EntitySpecificationBuilder<S, E> inverseRelationShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config);

    EntitySpecificationBuilder<S, E> indexShadow(String name,
            ToIntFunction<E> getter, ObjIntConsumer<E> setter, Consumer<SourceRefBuilder> config);

    <V> EntitySpecificationBuilder<S, E> previousElementShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config);

    <V> EntitySpecificationBuilder<S, E> nextElementShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config);

    <V> EntitySpecificationBuilder<S, E> declarativeShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter,
            Consumer<DeclarativeShadowBuilder<S, E, V>> config);

    <V> EntitySpecificationBuilder<S, E> cascadingUpdateShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter,
            Consumer<CascadingUpdateShadowBuilder<S, E>> config);

    EntitySpecificationBuilder<S, E> shadowVariablesInconsistent(String name,
            Function<E, Boolean> getter, BiConsumer<E, Boolean> setter);
}
