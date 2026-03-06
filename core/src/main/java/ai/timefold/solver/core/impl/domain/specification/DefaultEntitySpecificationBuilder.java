package ai.timefold.solver.core.impl.domain.specification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.domain.specification.CascadingUpdateShadowBuilder;
import ai.timefold.solver.core.api.domain.specification.DeclarativeShadowBuilder;
import ai.timefold.solver.core.api.domain.specification.EntitySpecification;
import ai.timefold.solver.core.api.domain.specification.EntitySpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.ListVariableSpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.ShadowSpecification;
import ai.timefold.solver.core.api.domain.specification.SourceRefBuilder;
import ai.timefold.solver.core.api.domain.specification.ValueRangeSpecification;
import ai.timefold.solver.core.api.domain.specification.VariableSpecification;
import ai.timefold.solver.core.api.domain.specification.VariableSpecificationBuilder;

final class DefaultEntitySpecificationBuilder<S, E> implements EntitySpecificationBuilder<S, E> {

    private final Class<E> entityClass;
    private Function<?, ?> planningIdGetter;
    private BiConsumer<?, Object> planningIdSetter;
    private Comparator<?> difficultyComparator;
    private Predicate<?> pinnedPredicate;
    private ToIntFunction<?> pinToIndexFunction;
    private final List<VariableSpecification<S>> variables = new ArrayList<>();
    private final List<ShadowSpecification<S>> shadows = new ArrayList<>();
    private final List<ValueRangeSpecification<S>> entityScopedValueRanges = new ArrayList<>();

    DefaultEntitySpecificationBuilder(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public <T extends Comparable<T>> EntitySpecificationBuilder<S, E> planningId(Function<E, T> getter) {
        this.planningIdGetter = getter;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> EntitySpecificationBuilder<S, E> planningId(Function<E, T> getter,
            BiConsumer<E, T> setter) {
        this.planningIdGetter = getter;
        this.planningIdSetter = (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter;
        return this;
    }

    @Override
    public EntitySpecificationBuilder<S, E> difficultyComparator(Comparator<E> comparator) {
        this.difficultyComparator = comparator;
        return this;
    }

    @Override
    public EntitySpecificationBuilder<S, E> pinned(Predicate<E> isPinned) {
        this.pinnedPredicate = isPinned;
        return this;
    }

    @Override
    public EntitySpecificationBuilder<S, E> pinToIndex(ToIntFunction<E> pinToIndex) {
        this.pinToIndexFunction = pinToIndex;
        return this;
    }

    @Override
    public EntitySpecificationBuilder<S, E> valueRange(String id, Function<E, ?> getter) {
        entityScopedValueRanges.add(new ValueRangeSpecification<>(id, getter, entityClass, true));
        return this;
    }

    @Override
    public <V> EntitySpecificationBuilder<S, E> variable(String name, Class<V> valueType,
            Consumer<VariableSpecificationBuilder<S, E, V>> config) {
        var builder = new DefaultVariableSpecificationBuilder<S, E, V>(name, valueType, false);
        config.accept(builder);
        variables.add(builder.build());
        return this;
    }

    @Override
    public <V> EntitySpecificationBuilder<S, E> listVariable(String name, Class<V> elementType,
            Consumer<ListVariableSpecificationBuilder<S, E, V>> config) {
        var builder = new DefaultListVariableSpecificationBuilder<S, E, V>(name, elementType);
        config.accept(builder);
        variables.add(builder.build());
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> EntitySpecificationBuilder<S, E> inverseRelationShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config) {
        var ref = new DefaultSourceRefBuilder();
        config.accept(ref);
        shadows.add(new ShadowSpecification.InverseRelation<>(name, type,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter,
                ref.getSourceVariableName()));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntitySpecificationBuilder<S, E> indexShadow(String name,
            ToIntFunction<E> getter, ObjIntConsumer<E> setter, Consumer<SourceRefBuilder> config) {
        var ref = new DefaultSourceRefBuilder();
        config.accept(ref);
        shadows.add(new ShadowSpecification.Index<>(name, Integer.class,
                (ToIntFunction<?>) getter,
                (ObjIntConsumer<?>) setter,
                ref.getSourceVariableName()));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> EntitySpecificationBuilder<S, E> previousElementShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config) {
        var ref = new DefaultSourceRefBuilder();
        config.accept(ref);
        shadows.add(new ShadowSpecification.PreviousElement<>(name, type,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter,
                ref.getSourceVariableName()));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> EntitySpecificationBuilder<S, E> nextElementShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter, Consumer<SourceRefBuilder> config) {
        var ref = new DefaultSourceRefBuilder();
        config.accept(ref);
        shadows.add(new ShadowSpecification.NextElement<>(name, type,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter,
                ref.getSourceVariableName()));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> EntitySpecificationBuilder<S, E> declarativeShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter,
            Consumer<DeclarativeShadowBuilder<S, E, V>> config) {
        var builder = new DefaultDeclarativeShadowBuilder<S, E, V>();
        config.accept(builder);
        shadows.add(new ShadowSpecification.Declarative<>(name, type,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter,
                builder.supplier,
                builder.sourcePaths != null ? List.copyOf(builder.sourcePaths) : List.of(),
                builder.alignmentKey));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> EntitySpecificationBuilder<S, E> cascadingUpdateShadow(String name, Class<V> type,
            Function<E, V> getter, BiConsumer<E, V> setter,
            Consumer<CascadingUpdateShadowBuilder<S, E>> config) {
        var builder = new DefaultCascadingUpdateShadowBuilder<S, E>();
        config.accept(builder);
        shadows.add(new ShadowSpecification.CascadingUpdate<>(name, type,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter,
                builder.updateMethod,
                builder.sourcePaths != null ? List.copyOf(builder.sourcePaths) : List.of()));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntitySpecificationBuilder<S, E> shadowVariablesInconsistent(String name,
            Function<E, Boolean> getter, BiConsumer<E, Boolean> setter) {
        shadows.add(new ShadowSpecification.Inconsistent<>(name, Boolean.class,
                (Function<?, ?>) getter,
                (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter));
        return this;
    }

    EntitySpecification<S> build() {
        return new EntitySpecification<>(
                entityClass,
                planningIdGetter,
                planningIdSetter,
                difficultyComparator,
                null,
                pinnedPredicate,
                pinToIndexFunction,
                List.copyOf(variables),
                List.copyOf(shadows),
                List.copyOf(entityScopedValueRanges));
    }

    private static final class DefaultDeclarativeShadowBuilder<S, E, V>
            implements DeclarativeShadowBuilder<S, E, V> {
        Function<?, ?> supplier;
        List<String> sourcePaths;
        String alignmentKey;

        @Override
        public DeclarativeShadowBuilder<S, E, V> supplier(Function<E, V> supplier) {
            this.supplier = supplier;
            return this;
        }

        @Override
        public DeclarativeShadowBuilder<S, E, V> sources(String... sourcePaths) {
            this.sourcePaths = List.of(sourcePaths);
            return this;
        }

        @Override
        public DeclarativeShadowBuilder<S, E, V> alignmentKey(String key) {
            this.alignmentKey = key;
            return this;
        }
    }

    private static final class DefaultCascadingUpdateShadowBuilder<S, E>
            implements CascadingUpdateShadowBuilder<S, E> {
        Consumer<?> updateMethod;
        List<String> sourcePaths;

        @Override
        @SuppressWarnings("unchecked")
        public CascadingUpdateShadowBuilder<S, E> updateMethod(Consumer<E> updater) {
            this.updateMethod = updater;
            return this;
        }

        @Override
        public CascadingUpdateShadowBuilder<S, E> sources(String... sourcePaths) {
            this.sourcePaths = List.of(sourcePaths);
            return this;
        }
    }
}
