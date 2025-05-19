package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.config.solver.PreviewFeature;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableLooped;

public class ShadowVariableLoopedVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {
    public ShadowVariableLoopedVariableDescriptor(int ordinal,
            EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        if (!descriptorPolicy.isPreviewFeatureEnabled(PreviewFeature.DECLARATIVE_SHADOW_VARIABLES)) {
            throw new IllegalStateException(
                    """
                            The member (%s) on the entity class (%s) has an (%s) annotation, but the declarative shadow variable preview feature is disabled.
                            Maybe enable declarative shadow variables in your %s?
                            """
                            .formatted(variableMemberAccessor.getName(), entityDescriptor.getEntityClass().getName(),
                                    ShadowVariableLooped.class.getSimpleName(), SolverConfig.class.getSimpleName()));
        }
    }

    @Override
    public List<VariableDescriptor<Solution_>> getSourceVariableDescriptorList() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        return Collections.emptyList();
    }

    @Override
    public Demand<?> getProvidedDemand() {
        return null;
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        return Collections.emptyList();
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        // no action needed
    }

    @Override
    public boolean isListVariableSource() {
        return false;
    }
}
