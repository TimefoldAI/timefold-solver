package ai.timefold.solver.core.impl.domain.variable.declarative;

public final class ExternalizedShadowVariableInconsistentProcessor<Solution_> {
    private final ShadowVariablesInconsistentVariableDescriptor<Solution_> shadowVariablesInconsistentVariableDescriptor;

    public ExternalizedShadowVariableInconsistentProcessor(
            ShadowVariablesInconsistentVariableDescriptor<Solution_> shadowVariablesInconsistentVariableDescriptor) {
        this.shadowVariablesInconsistentVariableDescriptor = shadowVariablesInconsistentVariableDescriptor;
    }

    Boolean getIsEntityInconsistent(Object entity) {
        return shadowVariablesInconsistentVariableDescriptor.getValue(entity);
    }

    void setIsEntityInconsistent(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity,
            boolean isInconsistent) {
        changedVariableNotifier.beforeVariableChanged().accept(shadowVariablesInconsistentVariableDescriptor, entity);
        shadowVariablesInconsistentVariableDescriptor.setValue(entity, isInconsistent);
        changedVariableNotifier.afterVariableChanged().accept(shadowVariablesInconsistentVariableDescriptor, entity);
    }
}
