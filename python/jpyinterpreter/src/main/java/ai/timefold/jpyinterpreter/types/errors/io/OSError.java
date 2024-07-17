package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class OSError extends PythonBaseException {
    final public static PythonLikeType OS_ERROR_TYPE =
            new PythonLikeType("OSError", OSError.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = OS_ERROR_TYPE;

    static {
        OS_ERROR_TYPE
                .setConstructor(((positionalArguments, namedArguments, callerInstance) -> new OSError(OS_ERROR_TYPE,
                        positionalArguments)));
    }

    public OSError(PythonLikeType type) {
        super(type);
    }

    public OSError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public OSError(PythonLikeType type, String message) {
        super(type, message);
    }
}
