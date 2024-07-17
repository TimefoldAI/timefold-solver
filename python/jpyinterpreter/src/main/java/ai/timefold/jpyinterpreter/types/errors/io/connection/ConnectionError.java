package ai.timefold.jpyinterpreter.types.errors.io.connection;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.io.OSError;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ConnectionError extends OSError {
    final public static PythonLikeType CONNECTION_ERROR_TYPE =
            new PythonLikeType("ConnectionError", ConnectionError.class, List.of(OS_ERROR_TYPE)),
            $TYPE = CONNECTION_ERROR_TYPE;

    static {
        CONNECTION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ConnectionError(CONNECTION_ERROR_TYPE,
                        positionalArguments)));
    }

    public ConnectionError(PythonLikeType type) {
        super(type);
    }

    public ConnectionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ConnectionError(PythonLikeType type, String message) {
        super(type, message);
    }
}
