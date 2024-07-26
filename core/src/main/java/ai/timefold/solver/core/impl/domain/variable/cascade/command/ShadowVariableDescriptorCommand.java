package ai.timefold.solver.core.impl.domain.variable.cascade.command;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;

public class ShadowVariableDescriptorCommand<Solution_> implements CascadingUpdateCommand<Object> {

    private final ShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public ShadowVariableDescriptorCommand(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    @Override
    public Object getValue(Object e) {
        return shadowVariableDescriptor.getValue(e);
    }
}
