package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface ListVariableChangeEvent<Entity_, Element_> extends ChangeEvent
        permits ListElementsChangeEvent, ListElementUnassignedChangeEvent {
}
