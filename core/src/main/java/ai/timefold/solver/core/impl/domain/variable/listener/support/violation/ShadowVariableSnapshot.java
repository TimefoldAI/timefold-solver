package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

final class ShadowVariableSnapshot {

    private final ShadowVariableDescriptor<?> shadowVariableDescriptor;
    private final Object entity;
    private final Object originalValue;

    private ShadowVariableSnapshot(ShadowVariableDescriptor<?> shadowVariableDescriptor, Object entity, Object originalValue) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.entity = entity;
        this.originalValue = originalValue;
    }

    static ShadowVariableSnapshot of(ShadowVariableDescriptor<?> shadowVariableDescriptor, Object entity) {
        return new ShadowVariableSnapshot(shadowVariableDescriptor, entity, shadowVariableDescriptor.getValue(entity));
    }

    void validate(Consumer<String> violationMessageConsumer) {
        Object newValue = shadowVariableDescriptor.getValue(entity);
        if (!Objects.equals(originalValue, newValue)) {
            violationMessageConsumer.accept(
                    """
                            The entity (%s)'s shadow variable (%s)'s corrupted value (%s) changed to uncorrupted value (%s) after all variable listeners were triggered without changes to the genuine variables.
                            Maybe one of the listeners (%s) for that shadow variable (%s) forgot to update it when one of its sourceVariables (%s) changed.
                            Or vice versa, maybe one of the listeners computes this shadow variable using a planning variable that is not declared as its source.
                            Use the repeatable @%s annotation for each source variable that is used to compute this shadow variable.
                            """
                            .formatted(
                                    entity,
                                    shadowVariableDescriptor.getSimpleEntityAndVariableName(),
                                    originalValue,
                                    newValue,
                                    shadowVariableDescriptor.getVariableListenerClasses().stream().map(Class::getSimpleName)
                                            .toList(),
                                    shadowVariableDescriptor.getSimpleEntityAndVariableName(),
                                    shadowVariableDescriptor.getSourceVariableDescriptorList().stream()
                                            .map(VariableDescriptor::getSimpleEntityAndVariableName)
                                            .collect(Collectors.toList()),
                                    ShadowVariable.class.getSimpleName()));
        }
    }

    ShadowVariableDescriptor<?> getShadowVariableDescriptor() {
        return shadowVariableDescriptor;
    }

    @Override
    public String toString() {
        return entity + "." + shadowVariableDescriptor.getVariableName() + " = " + originalValue;
    }
}
