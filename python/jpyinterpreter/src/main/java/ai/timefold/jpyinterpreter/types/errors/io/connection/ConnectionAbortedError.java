package ai.timefold.jpyinterpreter.types.errors.io.connection;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ConnectionAbortedError extends ConnectionError {
    final public static PythonLikeType CONNECTION_ABORTED_ERROR_TYPE =
            new PythonLikeType("ConnectionAbortedError", ConnectionAbortedError.class, List.of(CONNECTION_ERROR_TYPE)),
            $TYPE = CONNECTION_ABORTED_ERROR_TYPE;

    static {
        CONNECTION_ABORTED_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments,
                callerInstance) -> new ConnectionAbortedError(CONNECTION_ABORTED_ERROR_TYPE, positionalArguments)));
    }

    public ConnectionAbortedError(PythonLikeType type) {
        super(type);
    }

    public ConnectionAbortedError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ConnectionAbortedError(PythonLikeType type, String message) {
        super(type, message);
    }
}
