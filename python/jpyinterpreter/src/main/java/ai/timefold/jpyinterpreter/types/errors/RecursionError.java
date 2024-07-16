package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class RecursionError extends RuntimeError {
    final public static PythonLikeType RECURSION_ERROR_TYPE =
            new PythonLikeType("RecursionError", RecursionError.class, List.of(RUNTIME_ERROR_TYPE)),
            $TYPE = RECURSION_ERROR_TYPE;

    static {
        RECURSION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new RecursionError(RECURSION_ERROR_TYPE,
                        positionalArguments)));
    }

    public RecursionError(PythonLikeType type) {
        super(type);
    }

    public RecursionError(PythonLikeType type, String message) {
        super(type, message);
    }

    public RecursionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
