package ai.timefold.solver.core.impl.domain.variable;

public sealed interface ChangeEvent permits BasicVariableChangeEvent, ListVariableChangeEvent {
}
