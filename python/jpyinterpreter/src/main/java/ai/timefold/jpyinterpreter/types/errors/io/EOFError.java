package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class EOFError extends PythonBaseException {
    final public static PythonLikeType EOF_ERROR_TYPE =
            new PythonLikeType("EOFError", EOFError.class, List.of(PythonBaseException.BASE_EXCEPTION_TYPE)),
            $TYPE = EOF_ERROR_TYPE;

    static {
        EOF_ERROR_TYPE
                .setConstructor(((positionalArguments, namedArguments, callerInstance) -> new EOFError(EOF_ERROR_TYPE,
                        positionalArguments)));
    }

    public EOFError(PythonLikeType type) {
        super(type);
    }

    public EOFError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public EOFError(PythonLikeType type, String message) {
        super(type, message);
    }
}
