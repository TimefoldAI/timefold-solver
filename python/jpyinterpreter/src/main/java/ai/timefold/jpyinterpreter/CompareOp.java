package ai.timefold.jpyinterpreter;

import java.util.Objects;

public enum CompareOp {
    LESS_THAN("<", "__lt__"),
    LESS_THAN_OR_EQUALS("<=", "__le__"),
    EQUALS("==", "__eq__"),
    NOT_EQUALS("!=", "__ne__"),
    GREATER_THAN(">", "__gt__"),
    GREATER_THAN_OR_EQUALS(">=", "__ge__");

    public final String id;
    public final String dunderMethod;

    CompareOp(String id, String dunderMethod) {
        this.id = id;
        this.dunderMethod = dunderMethod;
    }

    public static CompareOp getOpForDunderMethod(String dunderMethod) {
        for (CompareOp op : CompareOp.values()) {
            if (op.dunderMethod.equals(dunderMethod)) {
                return op;
            }
        }
        throw new IllegalArgumentException("No Op corresponds to dunder method (" + dunderMethod + ")");
    }

    public static CompareOp getOp(String id) {
        for (CompareOp op : CompareOp.values()) {
            if (Objects.equals(op.id, id)) {
                return op;
            }
        }
        throw new IllegalArgumentException("No Op corresponds to id (" + id + ")");
    }
}
