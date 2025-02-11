package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.function.BiConsumer;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;

public class ChangedVariableNotifier {
    private final BiConsumer<VariableDescriptor<?>, Object> beforeVariableChanged;
    private final BiConsumer<VariableDescriptor<?>, Object> afterVariableChanged;

    public ChangedVariableNotifier(BiConsumer<VariableDescriptor<?>, Object> beforeVariableChanged,
            BiConsumer<VariableDescriptor<?>, Object> afterVariableChanged) {
        this.beforeVariableChanged = beforeVariableChanged;
        this.afterVariableChanged = afterVariableChanged;
    }

    public static ChangedVariableNotifier empty() {
        return new ChangedVariableNotifier(
                (vd, entity) -> {
                },
                (vd, entity) -> {
                });
    }

    public <Entity_> void beforeVariableChanged(VariableDescriptor<?> variableDescriptor, Entity_ entity) {
        beforeVariableChanged.accept(variableDescriptor, entity);
    }

    public <Entity_> void afterVariableChanged(VariableDescriptor<?> variableDescriptor, Entity_ entity) {
        afterVariableChanged.accept(variableDescriptor, entity);
    }
}
