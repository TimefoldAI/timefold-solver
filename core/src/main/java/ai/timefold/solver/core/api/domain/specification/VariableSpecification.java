package ai.timefold.solver.core.api.domain.specification;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Describes a planning variable on an entity.
 *
 * @param name the variable name
 * @param valueType the type of value this variable holds (or element type for list variables)
 * @param getter reads the variable value from the entity
 * @param setter writes the variable value to the entity
 * @param isList true if this is a list variable
 * @param allowsUnassigned true if the variable may remain unassigned
 * @param valueRangeRefs references to value range provider IDs
 * @param strengthComparator optional comparator for value strength sorting
 * @param strengthComparatorFactoryClass optional factory class for solution-aware value strength sorting
 * @param <S> the solution type
 */
public record VariableSpecification<S>(
        String name,
        Class<?> valueType,
        Function<?, ?> getter,
        BiConsumer<?, Object> setter,
        boolean isList,
        boolean allowsUnassigned,
        List<String> valueRangeRefs,
        Comparator<?> strengthComparator,
        Class<?> strengthComparatorFactoryClass) {

    /**
     * Backward-compatible constructor without comparatorFactoryClass.
     */
    public VariableSpecification(
            String name,
            Class<?> valueType,
            Function<?, ?> getter,
            BiConsumer<?, Object> setter,
            boolean isList,
            boolean allowsUnassigned,
            List<String> valueRangeRefs,
            Comparator<?> strengthComparator) {
        this(name, valueType, getter, setter, isList, allowsUnassigned, valueRangeRefs, strengthComparator, null);
    }
}
