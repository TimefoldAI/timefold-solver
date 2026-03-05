package ai.timefold.solver.core.api.domain.specification;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Describes a planning entity.
 *
 * @param entityClass the entity class
 * @param planningIdGetter optional getter for the planning ID
 * @param difficultyComparator optional comparator for entity difficulty sorting
 * @param difficultyComparatorFactoryClass optional factory class for solution-aware entity difficulty sorting
 * @param pinnedPredicate optional predicate to determine if an entity is pinned
 * @param pinToIndexFunction optional function to determine the pin-to-index
 * @param variables the genuine planning variables
 * @param shadows the shadow variables
 * @param entityScopedValueRanges value ranges scoped to this entity
 * @param <S> the solution type
 */
public record EntitySpecification<S>(
        Class<?> entityClass,
        Function<?, ?> planningIdGetter,
        java.util.function.BiConsumer<?, Object> planningIdSetter,
        Comparator<?> difficultyComparator,
        Class<?> difficultyComparatorFactoryClass,
        Predicate<?> pinnedPredicate,
        ToIntFunction<?> pinToIndexFunction,
        List<VariableSpecification<S>> variables,
        List<ShadowSpecification<S>> shadows,
        List<ValueRangeSpecification<S>> entityScopedValueRanges) {

    /**
     * Backward-compatible constructor without planningIdSetter and difficultyComparatorFactoryClass.
     */
    public EntitySpecification(
            Class<?> entityClass,
            Function<?, ?> planningIdGetter,
            Comparator<?> difficultyComparator,
            Predicate<?> pinnedPredicate,
            ToIntFunction<?> pinToIndexFunction,
            List<VariableSpecification<S>> variables,
            List<ShadowSpecification<S>> shadows,
            List<ValueRangeSpecification<S>> entityScopedValueRanges) {
        this(entityClass, planningIdGetter, null, difficultyComparator, null,
                pinnedPredicate, pinToIndexFunction, variables, shadows, entityScopedValueRanges);
    }

    /**
     * Backward-compatible constructor without planningIdSetter.
     */
    public EntitySpecification(
            Class<?> entityClass,
            Function<?, ?> planningIdGetter,
            Comparator<?> difficultyComparator,
            Class<?> difficultyComparatorFactoryClass,
            Predicate<?> pinnedPredicate,
            ToIntFunction<?> pinToIndexFunction,
            List<VariableSpecification<S>> variables,
            List<ShadowSpecification<S>> shadows,
            List<ValueRangeSpecification<S>> entityScopedValueRanges) {
        this(entityClass, planningIdGetter, null, difficultyComparator, difficultyComparatorFactoryClass,
                pinnedPredicate, pinToIndexFunction, variables, shadows, entityScopedValueRanges);
    }
}
