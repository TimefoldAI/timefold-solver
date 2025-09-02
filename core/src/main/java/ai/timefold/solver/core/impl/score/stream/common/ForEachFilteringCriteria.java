package ai.timefold.solver.core.impl.score.stream.common;

import java.util.function.Function;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum ForEachFilteringCriteria {
    ASSIGNED_AND_CONSISTENT(entityDescriptor -> entityDescriptor.getEntityForEachFilter().getAssignedAndConsistentPredicate()),
    CONSISTENT(entityDescriptor -> entityDescriptor.getEntityForEachFilter().getConsistentPredicate()),
    ALL(ignored -> null);

    private final Function<EntityDescriptor<?>, @Nullable Predicate<Object>> entityDescriptorToPredicateFunction;

    ForEachFilteringCriteria(Function<EntityDescriptor<?>, Predicate<Object>> entityDescriptorToPredicateFunction) {
        this.entityDescriptorToPredicateFunction = entityDescriptorToPredicateFunction;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <A> Predicate<A> getFilterForEntityDescriptor(EntityDescriptor<?> entityDescriptor) {
        return (Predicate<A>) entityDescriptorToPredicateFunction.apply(entityDescriptor);
    }
}
