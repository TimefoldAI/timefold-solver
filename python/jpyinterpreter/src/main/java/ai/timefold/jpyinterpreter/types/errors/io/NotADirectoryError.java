package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class NotADirectoryError extends OSError {
    final public static PythonLikeType NOT_A_DIRECTORY_ERROR_TYPE =
            new PythonLikeType("NotADirectoryError", NotADirectoryError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = NOT_A_DIRECTORY_ERROR_TYPE;

    static {
        NOT_A_DIRECTORY_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new NotADirectoryError(NOT_A_DIRECTORY_ERROR_TYPE, positionalArguments)));
    }

    public NotADirectoryError(PythonLikeType type) {
        super(type);
    }

    public NotADirectoryError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public NotADirectoryError(PythonLikeType type, String message) {
        super(type, message);
    }
}
