package ai.timefold.solver.core.impl.domain.variable.declarative;

public enum ParentVariableType {
    /**
     * A variable accessed from the root object.
     */
    NO_PARENT(false, false),

    /**
     * A variable accessed from another variable.
     */
    VARIABLE(false, false),

    /**
     * Variable on the inverse accessed from the root object.
     */
    INVERSE(false, false),

    /**
     * Variable on a next element variable accessed from the root object.
     */
    NEXT(true, false),

    /**
     * Variable on a previous element variable accessed from the root object.
     */
    PREVIOUS(true, false),

    /*
     * Previous element variable accessed from the root object in a chained model
     * (i.e. PlanningVariable(graphType = PlanningVariableGraphType.CHAINED))
     * is not included, since it would require a source path to accept properties
     * that are only included on subclasses of the property's type (since the
     * value of a chained value is either an entity (which has the property) or
     * an anchor (which does not have the property)).
     */

    /**
     * Variable on a next element variable accessed from the root object in a chained model.
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
