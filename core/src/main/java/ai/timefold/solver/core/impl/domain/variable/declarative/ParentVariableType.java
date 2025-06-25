package ai.timefold.solver.core.impl.domain.variable.declarative;

public enum ParentVariableType {
    /**
     * A variable accessed from the root object.
     */
    NO_PARENT(false, false),

    /**
     * A variable accessed from the inverse.
     */
    INVERSE(false, false),

    /**
     * Next element variable accessed from the root object.
     */
    NEXT(true, false),

    /**
     * Previous element variable accessed from the root object.
     */
    PREVIOUS(true, false),

    /**
     * PREVIOUS element variable accessed from the root object in a chained model.
     * Note: it might be impossible, since a chained model uses separate classes
     * for the anchor and values, and the anchor does not have a planning
     * variable on it.
     */
    CHAINED_PREVIOUS(true, false),

    /**
     * Next element variable accessed from the root object in a chained model.
     */
    CHAINED_NEXT(true, false),

    /**
     * A variable accessed indirectly from a fact or variable.
     */
    INDIRECT(false, true),

    /**
     * Variables accessed from a group.
     */
    GROUP(false, true);

    /**
     * True if the parent variable has a well-defined successor function.
     * For instance, the successor of a variable with a previous variable
     * is next.
     */
    private final boolean isDirectional;

    /**
     * True if the variable is accessed indirectly from a fact or
     * a group.
     */
    private final boolean isIndirect;

    ParentVariableType(boolean isDirectional, boolean isIndirect) {
        this.isDirectional = isDirectional;
        this.isIndirect = isIndirect;
    }

    public boolean isDirectional() {
        return isDirectional;
    }

    public boolean isIndirect() {
        return isIndirect;
    }
}
