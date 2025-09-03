package ai.timefold.solver.core.impl.domain.entity.descriptor;

import java.util.Collection;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.EntityConsistencyStateDemand;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class EntityForEachFilter {
    private final EntityDescriptor<?> entityDescriptor;
    private final Predicate<Object> assignedPredicate;
    private final boolean hasDeclarativeShadowVariables;

    EntityForEachFilter(EntityDescriptor<?> entityDescriptor) {
        var solutionDescriptor = entityDescriptor.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();

        this.entityDescriptor = entityDescriptor;
        this.assignedPredicate = getAssignedPredicate(entityDescriptor, listVariableDescriptor);
        this.hasDeclarativeShadowVariables = !getDeclarativeShadowVariables(entityDescriptor).isEmpty();
    }

    public Predicate<Object> getAssignedAndConsistentPredicate(SupplyManager supplyManager) {
        if (!hasDeclarativeShadowVariables) {
            return assignedPredicate;
        }
        var entityConsistencyState = supplyManager.demand(new EntityConsistencyStateDemand<>(entityDescriptor));
        return assignedPredicate.and(entityConsistencyState::isEntityConsistent);
    }

    @Nullable
    public Predicate<Object> getConsistentPredicate(SupplyManager supplyManager) {
        if (!hasDeclarativeShadowVariables) {
            return null;
        }
        var entityConsistencyState = supplyManager.demand(new EntityConsistencyStateDemand<>(entityDescriptor));
        return entityConsistencyState::isEntityConsistent;
    }

    private static Collection<? extends ShadowVariableDescriptor>
            getDeclarativeShadowVariables(EntityDescriptor<?> entityDescriptor) {
        return entityDescriptor.getShadowVariableDescriptors()
                .stream()
                .filter(shadowVariableDescriptor -> shadowVariableDescriptor instanceof DeclarativeShadowVariableDescriptor<?>)
                .toList();
    }

    private static Predicate<Object> getAssignedPredicate(EntityDescriptor<?> entityDescriptor,
            ListVariableDescriptor<?> listVariableDescriptor) {
        var isListVariableValue =
                listVariableDescriptor != null && listVariableDescriptor.acceptsValueType(entityDescriptor.getEntityClass());
        if (isListVariableValue) {
            var listInverseVariable = listVariableDescriptor.getInverseRelationShadowVariableDescriptor();
            if (listInverseVariable != null) {
                return entity -> entityDescriptor.hasNoNullVariables(entity) && listInverseVariable.getValue(entity) != null;
            } else {
                return ignored -> {
                    throw new IllegalStateException("""
                            Impossible state: assigned predicate for list variable value should not be used
                            when there is no inverse relation shadow variable descriptor.
                            """);
                };
            }
        } else {
            return entityDescriptor::hasNoNullVariables;
        }
    }
}
