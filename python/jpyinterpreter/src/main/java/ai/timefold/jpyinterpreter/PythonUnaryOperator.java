package ai.timefold.jpyinterpreter;

/**
 * The list of all Python Unary Operators, which take
 * self as the only argument.
 *
 * ex: a.__neg__()
 */
public enum PythonUnaryOperator {
    NEGATIVE("__neg__"),
    POSITIVE("__pos__"),
    ABS("__abs__"),
    INVERT("__invert__"),
    ITERATOR("__iter__"),
    AS_BOOLEAN("__bool__"),
    AS_FLOAT("__float__"),
    AS_INT("__int__"),
    AS_INDEX("__index__"),
    AS_STRING("__str__"),
    REPRESENTATION("__repr__"),
    ENTER("__enter__"),
    LENGTH("__len__"),
    NEXT("__next__"),
    REVERSED("__reversed__"),
    HASH("__hash__");

    final String dunderMethod;

    PythonUnaryOperator(String dunderMethod) {
        this.dunderMethod = dunderMethod;
    }

    public String getDunderMethod() {
        return dunderMethod;
    }
}
