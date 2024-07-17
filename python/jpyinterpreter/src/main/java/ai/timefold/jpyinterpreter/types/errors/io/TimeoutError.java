package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class TimeoutError extends OSError {
    final public static PythonLikeType TIMEOUT_ERROR_TYPE =
            new PythonLikeType("TimeoutError", TimeoutError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = TIMEOUT_ERROR_TYPE;

    static {
        TIMEOUT_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new TimeoutError(TIMEOUT_ERROR_TYPE,
                        positionalArguments)));
    }

    public TimeoutError(PythonLikeType type) {
        super(type);
    }

    public TimeoutError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public TimeoutError(PythonLikeType type, String message) {
        super(type, message);
    }
}
