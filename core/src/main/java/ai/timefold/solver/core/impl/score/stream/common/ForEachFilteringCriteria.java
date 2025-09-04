package ai.timefold.solver.core.impl.score.stream.common;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.ConsistencyTracker;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum ForEachFilteringCriteria {
    ASSIGNED_AND_CONSISTENT((consistencyTracker, entityDescriptor) -> entityDescriptor.getEntityForEachFilter()
            .getAssignedAndConsistentPredicate(consistencyTracker)),
    CONSISTENT((consistencyTracker, entityDescriptor) -> entityDescriptor.getEntityForEachFilter()
            .getConsistentPredicate(consistencyTracker)),
    ALL((ignored1, ignored2) -> null);

    private final BiFunction<ConsistencyTracker<?>, EntityDescriptor<?>, @Nullable Predicate<Object>> entityDescriptorToPredicateFunction;

    ForEachFilteringCriteria(
            BiFunction<ConsistencyTracker<?>, EntityDescriptor<?>, Predicate<Object>> entityDescriptorToPredicateFunction) {
        this.entityDescriptorToPredicateFunction = entityDescriptorToPredicateFunction;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <A> Predicate<A> getFilterForEntityDescriptor(ConsistencyTracker<?> consistencyTracker,
            EntityDescriptor<?> entityDescriptor) {
        return (Predicate<A>) entityDescriptorToPredicateFunction.apply(consistencyTracker, entityDescriptor);
    }
}
