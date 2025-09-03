package ai.timefold.solver.core.impl.score.stream.common;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum ForEachFilteringCriteria {
    ASSIGNED_AND_CONSISTENT((supplyManager, entityDescriptor) -> entityDescriptor.getEntityForEachFilter()
            .getAssignedAndConsistentPredicate(supplyManager)),
    CONSISTENT((supplyManager, entityDescriptor) -> entityDescriptor.getEntityForEachFilter()
            .getConsistentPredicate(supplyManager)),
    ALL((ignored1, ignored2) -> null);

    private final BiFunction<SupplyManager, EntityDescriptor<?>, @Nullable Predicate<Object>> entityDescriptorToPredicateFunction;

    ForEachFilteringCriteria(
            BiFunction<SupplyManager, EntityDescriptor<?>, Predicate<Object>> entityDescriptorToPredicateFunction) {
        this.entityDescriptorToPredicateFunction = entityDescriptorToPredicateFunction;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <A> Predicate<A> getFilterForEntityDescriptor(SupplyManager supplyManager, EntityDescriptor<?> entityDescriptor) {
        return (Predicate<A>) entityDescriptorToPredicateFunction.apply(supplyManager, entityDescriptor);
    }
}
