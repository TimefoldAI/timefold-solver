package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class BufferError extends PythonException {
    final public static PythonLikeType BUFFER_ERROR_TYPE =
            new PythonLikeType("BufferError", BufferError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = BUFFER_ERROR_TYPE;

    static {
        BUFFER_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new BufferError(BUFFER_ERROR_TYPE,
                        positionalArguments)));
    }

    public BufferError(PythonLikeType type) {
        super(type);
    }

    public BufferError(PythonLikeType type, String message) {
        super(type, message);
    }

    public BufferError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
