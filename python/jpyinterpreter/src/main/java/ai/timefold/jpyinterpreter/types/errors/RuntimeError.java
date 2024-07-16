package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class RuntimeError extends PythonBaseException {
    final public static PythonLikeType RUNTIME_ERROR_TYPE =
            new PythonLikeType("RuntimeError", RuntimeError.class, List.of(BASE_EXCEPTION_TYPE)),
            $TYPE = RUNTIME_ERROR_TYPE;

    static {
        RUNTIME_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new RuntimeError(RUNTIME_ERROR_TYPE,
                        positionalArguments)));
    }

    public RuntimeError(String message) {
        super(RUNTIME_ERROR_TYPE, message);
    }

    public RuntimeError(PythonLikeType type) {
        super(type);
    }

    public RuntimeError(PythonLikeType type, String message) {
        super(type, message);
    }

    public RuntimeError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
