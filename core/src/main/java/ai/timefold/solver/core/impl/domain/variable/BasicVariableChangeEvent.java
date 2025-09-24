package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record BasicVariableChangeEvent<Entity_>(
        Entity_ entity) implements ChangeEvent {
}
