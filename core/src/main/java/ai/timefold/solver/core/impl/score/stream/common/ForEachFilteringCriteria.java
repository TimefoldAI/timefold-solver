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

    private final CriteriaFunction<Object, Object> criteriaFunction;

    ForEachFilteringCriteria(CriteriaFunction<Object, Object> criteriaFunction) {
        this.criteriaFunction = criteriaFunction;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <Solution_, A> Predicate<A> getFilterForEntityDescriptor(ConsistencyTracker<Solution_> consistencyTracker,
            EntityDescriptor<Solution_> entityDescriptor) {
        return (Predicate<A>) criteriaFunction.apply((ConsistencyTracker<Object>) consistencyTracker,
                (EntityDescriptor<Object>) entityDescriptor);
    }

    @FunctionalInterface
    private interface CriteriaFunction<Solution_, A>
            extends BiFunction<ConsistencyTracker<Solution_>, EntityDescriptor<Solution_>, @Nullable Predicate<A>> {
    }

}
