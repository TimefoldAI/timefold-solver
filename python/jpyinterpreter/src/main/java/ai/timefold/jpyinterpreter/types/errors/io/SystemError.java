package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class SystemError extends PythonBaseException {
    final public static PythonLikeType SYSTEM_ERROR_TYPE =
            new PythonLikeType("SystemError", SystemError.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = SYSTEM_ERROR_TYPE;

    static {
        SYSTEM_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new SystemError(SYSTEM_ERROR_TYPE,
                        positionalArguments)));
    }

    public SystemError(PythonLikeType type) {
        super(type);
    }

    public SystemError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public SystemError(PythonLikeType type, String message) {
        super(type, message);
    }
}
