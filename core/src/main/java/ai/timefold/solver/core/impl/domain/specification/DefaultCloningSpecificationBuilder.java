package ai.timefold.solver.core.impl.domain.specification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.specification.CloningSpecification;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.CloneableClassDescriptor;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.DeepCloneDecision;
import ai.timefold.solver.core.api.domain.specification.CloningSpecification.PropertyCopyDescriptor;
import ai.timefold.solver.core.api.domain.specification.CloningSpecificationBuilder;

final class DefaultCloningSpecificationBuilder<S> implements CloningSpecificationBuilder<S> {

    private Supplier<S> solutionFactory;
    private final List<PropertyCopyDescriptor> solutionProperties = new ArrayList<>();
    private final Map<Class<?>, CloneableClassDescriptor> cloneableClasses = new LinkedHashMap<>();
    private final Set<Class<?>> entityClasses = new LinkedHashSet<>();
    private final Set<Class<?>> deepCloneClasses = new LinkedHashSet<>();

    @Override
    public CloningSpecificationBuilder<S> solutionFactory(Supplier<S> factory) {
        this.solutionFactory = factory;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> CloningSpecificationBuilder<S> solutionProperty(String name, Function<S, V> getter,
            BiConsumer<S, V> setter) {
        return solutionProperty(name, getter, setter, DeepCloneDecision.SHALLOW);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> CloningSpecificationBuilder<S> solutionProperty(String name, Function<S, V> getter,
            BiConsumer<S, V> setter, DeepCloneDecision decision) {
        solutionProperties.add(new PropertyCopyDescriptor(
                name,
                (Function<Object, Object>) (Function<?, ?>) getter,
                (BiConsumer<Object, Object>) (BiConsumer<?, ?>) setter,
                decision));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> CloningSpecificationBuilder<S> entityClass(Class<E> entityClass, Supplier<E> factory,
            Consumer<CloneableClassBuilder<E>> config) {
        entityClasses.add(entityClass);
        var builder = new DefaultCloneableClassBuilder<E>();
        config.accept(builder);
        cloneableClasses.put(entityClass, new CloneableClassDescriptor(
                entityClass, (Supplier<Object>) (Supplier<?>) factory,
                List.copyOf(builder.properties)));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> CloningSpecificationBuilder<S> deepCloneFact(Class<E> factClass, Supplier<E> factory,
            Consumer<CloneableClassBuilder<E>> config) {
        deepCloneClasses.add(factClass);
        var builder = new DefaultCloneableClassBuilder<E>();
        config.accept(builder);
        cloneableClasses.put(factClass, new CloneableClassDescriptor(
                factClass, (Supplier<Object>) (Supplier<?>) factory,
                List.copyOf(builder.properties)));
        return this;
    }

    CloningSpecification<S> build() {
        return new CloningSpecification<>(
                solutionFactory,
                List.copyOf(solutionProperties),
                Map.copyOf(cloneableClasses),
                Set.copyOf(entityClasses),
                Set.copyOf(deepCloneClasses),
                null);
    }

    private static final class DefaultCloneableClassBuilder<E> implements CloneableClassBuilder<E> {

        private final List<PropertyCopyDescriptor> properties = new ArrayList<>();

        @Override
        public <V> CloneableClassBuilder<E> shallowProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.SHALLOW);
        }

        @Override
        public <V> CloneableClassBuilder<E> entityRefProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.RESOLVE_ENTITY_REFERENCE);
        }

        @Override
        public <V> CloneableClassBuilder<E> deepProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.ALWAYS_DEEP);
        }

        @Override
        public <V> CloneableClassBuilder<E> deepCollectionProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.DEEP_COLLECTION);
        }

        @Override
        public <V> CloneableClassBuilder<E> deepMapProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.DEEP_MAP);
        }

        @Override
        public <V> CloneableClassBuilder<E> deepArrayProperty(String name, Function<E, V> getter,
                BiConsumer<E, V> setter) {
            return property(name, getter, setter, DeepCloneDecision.DEEP_ARRAY);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> CloneableClassBuilder<E> property(String name, Function<E, V> getter,
                BiConsumer<E, V> setter, DeepCloneDecision decision) {
            properties.add(new PropertyCopyDescriptor(
                    name,
                    (Function<Object, Object>) (Function<?, ?>) getter,
                    (BiConsumer<Object, Object>) (BiConsumer<?, ?>) setter,
                    decision));
            return this;
        }
    }
}
