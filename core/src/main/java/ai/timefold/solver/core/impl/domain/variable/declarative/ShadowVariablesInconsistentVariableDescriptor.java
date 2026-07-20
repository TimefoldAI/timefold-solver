package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.policy.DescriptorPolicy;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Demand;

public class ShadowVariablesInconsistentVariableDescriptor<Solution_> extends ShadowVariableDescriptor<Solution_> {
    public ShadowVariablesInconsistentVariableDescriptor(int ordinal,
            EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    public void processAnnotations(DescriptorPolicy descriptorPolicy) {
        // no action needed
    }

    @Override
    public Collection<Class<?>> getUpdaterClasses() {
        return Collections.emptyList();
    }

    @Override
    public Demand<?> getProvidedDemand() {
        return null;
    }

    @Override
    public void linkVariableDescriptors(DescriptorPolicy descriptorPolicy) {
        // no action needed
    }

}
