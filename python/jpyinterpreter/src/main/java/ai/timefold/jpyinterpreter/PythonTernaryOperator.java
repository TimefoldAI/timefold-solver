package ai.timefold.jpyinterpreter;

/**
 * The list of all Python Ternary Operators, which take
 * self and two other arguments.
 *
 * ex: a.__setitem__(key, value)
 */
public enum PythonTernaryOperator {
    // Descriptor operations
    // https://docs.python.org/3/howto/descriptor.html
    GET("__get__"),
    SET("__set__"),

    // Attribute access
    SET_ATTRIBUTE("__setattr__"),

    // List operations
    // https://docs.python.org/3/reference/datamodel.html#object.__setitem__
    SET_ITEM("__setitem__");

    public final String dunderMethod;

    PythonTernaryOperator(String dunderMethod) {
        this.dunderMethod = dunderMethod;
    }

    public String getDunderMethod() {
        return dunderMethod;
    }
}
