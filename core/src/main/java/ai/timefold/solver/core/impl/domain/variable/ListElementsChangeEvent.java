package ai.timefold.solver.core.impl.domain.variable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ListElementsChangeEvent<Entity_, Element_>(Entity_ entity, int changeStartIndexInclusive,
        int changeEndIndexExclusive) implements ListVariableChangeEvent<Entity_, Element_> {
}
