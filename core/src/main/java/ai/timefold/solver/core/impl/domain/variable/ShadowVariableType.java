package ai.timefold.solver.core.impl.domain.variable;

public enum ShadowVariableType {
    BASIC, // index, inverse element, anchor element, previous element, and next element
    CASCADING_UPDATE,
    DECLARATIVE
}
