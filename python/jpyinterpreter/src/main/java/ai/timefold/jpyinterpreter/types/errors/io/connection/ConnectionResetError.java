package ai.timefold.jpyinterpreter.types.errors.io.connection;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ConnectionResetError extends ConnectionError {
    final public static PythonLikeType CONNECTION_RESET_ERROR_TYPE =
            new PythonLikeType("ConnectionResetError", ConnectionResetError.class, List.of(CONNECTION_ERROR_TYPE)),
            $TYPE = CONNECTION_RESET_ERROR_TYPE;

    static {
        CONNECTION_RESET_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new ConnectionResetError(CONNECTION_RESET_ERROR_TYPE, positionalArguments)));
    }

    public ConnectionResetError(PythonLikeType type) {
        super(type);
    }

    public ConnectionResetError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ConnectionResetError(PythonLikeType type, String message) {
        super(type, message);
    }
}
