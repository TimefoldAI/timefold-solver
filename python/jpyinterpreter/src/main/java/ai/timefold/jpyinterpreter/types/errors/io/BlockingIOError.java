package ai.timefold.jpyinterpreter.types.errors.io;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class BlockingIOError extends OSError {
    final public static PythonLikeType BLOCKING_IO_ERROR_TYPE =
            new PythonLikeType("BlockingIOError", BlockingIOError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = BLOCKING_IO_ERROR_TYPE;

    static {
        BLOCKING_IO_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new BlockingIOError(BLOCKING_IO_ERROR_TYPE,
                        positionalArguments)));
    }

    public BlockingIOError(PythonLikeType type) {
        super(type);
    }

    public BlockingIOError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public BlockingIOError(PythonLikeType type, String message) {
        super(type, message);
    }
}
