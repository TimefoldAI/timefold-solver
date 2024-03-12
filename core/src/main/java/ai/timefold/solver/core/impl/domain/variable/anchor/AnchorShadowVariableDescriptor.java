package ai.timefold.solver.core.impl.domain.variable.anchor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.AnchorShadowVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableDemand;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class AnchorShadowVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {

    private VariableDescriptor<Solution_> sourceVariableDescriptor;

    public AnchorShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
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
        AnchorShadowVariable shadowVariableAnnotation = variableMemberAccessor.getAnnotation(AnchorShadowVariable.class);
        String sourceVariableName = shadowVariableAnnotation.sourceVariableName();
        sourceVariableDescriptor = entityDescriptor.getVariableDescriptor(sourceVariableName);
        if (sourceVariableDescriptor == null) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has an @" + AnchorShadowVariable.class.getSimpleName()
                    + " annotated property (" + variableMemberAccessor.getName()
                    + ") with sourceVariableName (" + sourceVariableName
                    + ") which is not a valid planning variable on entityClass ("
                    + entityDescriptor.getEntityClass() + ").\n"
                    + entityDescriptor.buildInvalidVariableNameExceptionMessage(sourceVariableName));
        }
        if (!(sourceVariableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor) ||
                !basicVariableDescriptor.isChained()) {
            throw new IllegalArgumentException("The entityClass (" + entityDescriptor.getEntityClass()
                    + ") has an @" + AnchorShadowVariable.class.getSimpleName()
                    + " annotated property (" + variableMemberAccessor.getName()
                    + ") with sourceVariableName (" + sourceVariableName
                    + ") which is not chained.");
        }
        sourceVariableDescriptor.registerSinkVariableDescriptor(this);
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.singletonList(sourceVariableDescriptor);
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.singleton(AnchorVariableListener.class);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public AnchorVariableDemand<Solution_> getProvidedDemand() {
        return new AnchorVariableDemand<>(sourceVariableDescriptor);
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        SingletonInverseVariableSupply inverseVariableSupply = supplyManager
                .demand(new SingletonInverseVariableDemand<>(sourceVariableDescriptor));
        return new VariableListenerWithSources<>(
                new AnchorVariableListener<>(this, sourceVariableDescriptor, inverseVariableSupply),
                sourceVariableDescriptor).toCollection();
    }

}
