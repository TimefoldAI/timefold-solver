package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ListElementUnassignedChangeEvent<Entity_, Element_>(
        Element_ element) implements ListVariableChangeEvent<Entity_, Element_> {
}
