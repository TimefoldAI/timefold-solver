package ai.timefold.solver.core.impl.domain.variable.index;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class IndexShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public IndexShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
        if (!variableMemberAccessor.getType().equals(Integer.class) && !variableMemberAccessor.getType().equals(Long.class)) {
            throw new IllegalStateException(
                    """
                            The entityClass (%s) has an @%s-annotated member (%s) of type (%s) which cannot represent an index in a list.
                            The @%s-annotated member type must be %s or %s."""
                            .formatted(entityDescriptor.getEntityClass().getName(), IndexShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor, variableMemberAccessor.getType(),
                                    IndexShadowVariable.class.getSimpleName(), Integer.class, Long.class));
        }
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // Do nothing
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        linkShadowSources(descriptorPolicy);
    }

    private void linkShadowSources(DescriptorPolicy descriptorPolicy) {
        String sourceVariableName = variableMemberAccessor.getAnnotation(IndexShadowVariable.class).sourceVariableName();
        List<EntityDescriptor<Solution_>> entitiesWithSourceVariable =
                entityDescriptor.getSolutionDescriptor().getEntityDescriptors().stream()
                        .filter(entityDescriptor -> entityDescriptor.hasVariableDescriptor(sourceVariableName))
                        .toList();
        if (entitiesWithSourceVariable.isEmpty()) {
            throw new IllegalArgumentException("""
                    The entityClass (%s) has an @%s-annotated property (%s) with sourceVariableName (%s) \
                    which is not a valid planning variable on any of the entity classes (%s)."""
                    .formatted(entityDescriptor.getEntityClass(), IndexShadowVariable.class.getSimpleName(),
                            variableMemberAccessor, sourceVariableName,
                            entityDescriptor.getSolutionDescriptor().getEntityDescriptors()));
        }
        if (entitiesWithSourceVariable.size() > 1) {
            throw new IllegalArgumentException("""
                    The entityClass (%s) has an @%s-annotated property (%s) with sourceVariableName (%s) \
                    which is not a unique planning variable.
                    A planning variable with the name (%s) exists on multiple entity classes (%s)."""
                    .formatted(entityDescriptor.getEntityClass(), IndexShadowVariable.class.getSimpleName(),
                            variableMemberAccessor, sourceVariableName, sourceVariableName, entitiesWithSourceVariable));
        }
        VariableDescriptor<Solution_> variableDescriptor =
                entitiesWithSourceVariable.get(0).getVariableDescriptor(sourceVariableName);
        if (variableDescriptor == null) {
            throw new IllegalStateException("""
                    Impossible state: variableDescriptor (%s) is null but previous checks indicate that \
                    the entityClass (%s) has a planning variable with sourceVariableName (%s)."""
                    .formatted(variableDescriptor, entityDescriptor.getEntityClass(), sourceVariableName));
        }
        if (!(variableDescriptor instanceof ListVariableDescriptor)) {
            throw new IllegalArgumentException(
                    "The entityClass (%s) has an @%s-annotated property (%s) with sourceVariableName (%s) which is not a @%s."
                            .formatted(entityDescriptor.getEntityClass(), IndexShadowVariable.class.getSimpleName(),
                                    variableMemberAccessor, sourceVariableName, PlanningListVariable.class.getSimpleName()));
        }
        sourceVariableDescriptor = (ListVariableDescriptor<Solution_>) variableDescriptor;
        sourceVariableDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.singletonList(sourceVariableDescriptor);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        throw new UnsupportedOperationException("Impossible state: Handled by %s."
                .formatted(ListVariableStateSupply.class.getSimpleName()));
    }

    @Override
    public Demand<?> getProvidedDemand() {
        throw new UnsupportedOperationException("Impossible state: Handled by %s."
                .formatted(ListVariableStateSupply.class.getSimpleName()));
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        throw new UnsupportedOperationException("Impossible state: Handled by %s."
                .formatted(ListVariableStateSupply.class.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer getValue(Object entity) {
        return super.getValue(entity);
    }

    @Override
    public boolean isListVariableSource() {
        return true;
    }
}
