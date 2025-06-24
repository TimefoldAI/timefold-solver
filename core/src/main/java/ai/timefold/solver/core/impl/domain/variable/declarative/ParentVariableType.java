package ai.timefold.solver.core.impl.domain.variable.declarative;

public enum ParentVariableType {
    /**
     * Next element variable accessed from the root object.
     */
    NEXT,

    /**
     * Previous element variable accessed from the root object.
     */
    PREVIOUS,

    /**
     * Previous element variable accessed from the root object in a chained model.
     */
    CHAINED_INVERSE,

    /**
     * Next/previous element variable accessed
     * from a fact.
     */
    INDIRECT_DIRECTIONAL,

    /**
     * Variables accessed from a group.
     */
    GROUP,

    /**
     * Declarative variables, inverse variables, and
     * genuine variables.
     */
    UNDIRECTIONAL
}
