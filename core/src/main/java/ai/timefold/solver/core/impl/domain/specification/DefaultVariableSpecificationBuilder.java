package ai.timefold.solver.core.impl.domain.specification;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.specification.VariableSpecification;
import ai.timefold.solver.core.api.domain.specification.VariableSpecificationBuilder;

final class DefaultVariableSpecificationBuilder<S, E, V> implements VariableSpecificationBuilder<S, E, V> {

    private final String name;
    private final Class<V> valueType;
    private final boolean isList;
    private Function<?, ?> getter;
    private BiConsumer<?, Object> setter;
    private List<String> valueRangeRefs = List.of();
    private boolean allowsUnassigned;
    private Comparator<?> strengthComparator;

    DefaultVariableSpecificationBuilder(String name, Class<V> valueType, boolean isList) {
        this.name = name;
        this.valueType = valueType;
        this.isList = isList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public VariableSpecificationBuilder<S, E, V> accessors(Function<E, V> getter, BiConsumer<E, V> setter) {
        this.getter = getter;
        this.setter = (BiConsumer<?, Object>) (BiConsumer<?, ?>) setter;
        return this;
    }

    @Override
    public VariableSpecificationBuilder<S, E, V> valueRange(String... refs) {
        this.valueRangeRefs = List.of(refs);
        return this;
    }

    @Override
    public VariableSpecificationBuilder<S, E, V> allowsUnassigned(boolean allows) {
        this.allowsUnassigned = allows;
        return this;
    }

    @Override
    public VariableSpecificationBuilder<S, E, V> strengthComparator(Comparator<V> comparator) {
        this.strengthComparator = comparator;
        return this;
    }

    VariableSpecification<S> build() {
        if (getter == null || setter == null) {
            throw new IllegalStateException(
                    "Variable '%s' requires accessors. Call accessors() before building.".formatted(name));
        }
        return new VariableSpecification<>(name, valueType, getter, setter,
                isList, allowsUnassigned, valueRangeRefs, strengthComparator);
    }
}
