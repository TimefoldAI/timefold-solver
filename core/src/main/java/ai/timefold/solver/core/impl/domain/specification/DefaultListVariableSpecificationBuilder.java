package ai.timefold.solver.core.impl.domain.specification;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.specification.ListVariableSpecificationBuilder;
import ai.timefold.solver.core.api.domain.specification.VariableSpecification;

final class DefaultListVariableSpecificationBuilder<S, E, V> implements ListVariableSpecificationBuilder<S, E, V> {

    private final String name;
    private final Class<V> elementType;
    private Function<?, ?> getter;
    private BiConsumer<?, Object> setter;
    private List<String> valueRangeRefs = List.of();
    private boolean allowsUnassignedValues;
    private Comparator<?> strengthComparator;

    DefaultListVariableSpecificationBuilder(String name, Class<V> elementType) {
        this.name = name;
        this.elementType = elementType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ListVariableSpecificationBuilder<S, E, V> accessors(Function<E, List<V>> getter,
            BiConsumer<E, List<V>> setter) {
        this.getter = getter;
        this.setter = (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter;
        return this;
    }

    @Override
    public ListVariableSpecificationBuilder<S, E, V> valueRange(String... refs) {
        this.valueRangeRefs = List.of(refs);
        return this;
    }

    @Override
    public ListVariableSpecificationBuilder<S, E, V> allowsUnassignedValues(boolean allows) {
        this.allowsUnassignedValues = allows;
        return this;
    }

    @Override
    public ListVariableSpecificationBuilder<S, E, V> strengthComparator(Comparator<V> comparator) {
        this.strengthComparator = comparator;
        return this;
    }

    VariableSpecification<S> build() {
        if (getter == null || setter == null) {
            throw new IllegalStateException(
                    "List variable '%s' requires accessors. Call accessors() before building.".formatted(name));
        }
        return new VariableSpecification<>(name, elementType, getter, setter,
                true, allowsUnassignedValues, valueRangeRefs, strengthComparator);
    }
}
