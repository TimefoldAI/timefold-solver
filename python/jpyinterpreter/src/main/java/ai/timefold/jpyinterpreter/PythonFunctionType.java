package ai.timefold.jpyinterpreter;

public enum PythonFunctionType {
    /**
     * A normal function that corresponds to a typical Java Function.
     */
    FUNCTION,

    /**
     * A generator function that corresponds to a Java Iterable (has yield opcodes)
     */
    GENERATOR
}
