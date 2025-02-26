package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.function.BiConsumer;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public class ChangedVariableNotifier<Solution_> {
    private static final ChangedVariableNotifier<?> EMPTY = new ChangedVariableNotifier<>((a, b) -> {
    },
            (a, b) -> {
            });

    private final BiConsumer<VariableDescriptor<Solution_>, Object> beforeVariableChanged;
    private final BiConsumer<VariableDescriptor<Solution_>, Object> afterVariableChanged;

    public ChangedVariableNotifier(BiConsumer<VariableDescriptor<Solution_>, Object> beforeVariableChanged,
            BiConsumer<VariableDescriptor<Solution_>, Object> afterVariableChanged) {
        this.beforeVariableChanged = beforeVariableChanged;
        this.afterVariableChanged = afterVariableChanged;
    }

    @SuppressWarnings("unchecked")
    public static <Solution_> ChangedVariableNotifier<Solution_> empty() {
        return (ChangedVariableNotifier<Solution_>) EMPTY;
    }

    public static <Solution_> ChangedVariableNotifier<Solution_> of(InnerScoreDirector<Solution_, ?> scoreDirector) {
        return new ChangedVariableNotifier<>(
                scoreDirector::beforeVariableChanged,
                scoreDirector::afterVariableChanged);
    }

    public <Entity_> void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Entity_ entity) {
        beforeVariableChanged.accept(variableDescriptor, entity);
    }

    public <Entity_> void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Entity_ entity) {
        afterVariableChanged.accept(variableDescriptor, entity);
    }
}
