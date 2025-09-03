package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.ref.WeakReference;
import java.util.Objects;

public final class ExternalizedShadowVariableInconsistentProcessor<Solution_> {
    // ExternalizedShadowVariableInconsistentProcessor is held by EntityConsistencyState,
    // which is stored in a static field since SupplyManager cannot be used since
    // EntityConsistencyState is used by cached Constraints.
    // As such, use a WeakReference to allow the entity descriptor to be garbage collected,
    // which in turn removes the EntityConsistencyState from the static field
    // (as it the value of a weak-referenced entity descriptor in a map).
    private final WeakReference<ShadowVariablesInconsistentVariableDescriptor<Solution_>> shadowVariablesInconsistentVariableDescriptorReference;

    public ExternalizedShadowVariableInconsistentProcessor(
            ShadowVariablesInconsistentVariableDescriptor<Solution_> shadowVariablesInconsistentVariableDescriptor) {
        this.shadowVariablesInconsistentVariableDescriptorReference =
                new WeakReference<>(shadowVariablesInconsistentVariableDescriptor);
    }

    public Boolean getIsEntityInconsistent(Object entity) {
        return Objects.requireNonNull(shadowVariablesInconsistentVariableDescriptorReference.get()).getValue(entity);
    }

    public void setIsEntityInconsistent(ChangedVariableNotifier<Solution_> changedVariableNotifier, Object entity,
            boolean isInconsistent) {
        var shadowVariablesInconsistentVariableDescriptor =
                Objects.requireNonNull(shadowVariablesInconsistentVariableDescriptorReference.get());
        changedVariableNotifier.beforeVariableChanged().accept(shadowVariablesInconsistentVariableDescriptor, entity);
        shadowVariablesInconsistentVariableDescriptor.setValue(entity, isInconsistent);
        changedVariableNotifier.afterVariableChanged().accept(shadowVariablesInconsistentVariableDescriptor, entity);
    }
}
