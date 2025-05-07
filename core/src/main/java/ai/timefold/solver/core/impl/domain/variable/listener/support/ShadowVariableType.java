package ai.timefold.solver.core.impl.domain.variable.listener.support;

public enum ShadowVariableType {
    BASIC, // index, inverse element, anchor element, previous element, and next element
    CUSTOM_LISTENER,
    CASCADING_UPDATE,
    DECLARATIVE
}
