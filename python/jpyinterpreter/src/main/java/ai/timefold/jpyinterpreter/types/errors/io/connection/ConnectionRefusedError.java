package ai.timefold.jpyinterpreter.types.errors.io.connection;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ConnectionRefusedError extends ConnectionError {
    final public static PythonLikeType CONNECTION_REFUSED_ERROR_TYPE =
            new PythonLikeType("ConnectionRefusedError", ConnectionRefusedError.class, List.of(CONNECTION_ERROR_TYPE)),
            $TYPE = CONNECTION_REFUSED_ERROR_TYPE;

    static {
        CONNECTION_REFUSED_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments,
                callerInstance) -> new ConnectionRefusedError(CONNECTION_REFUSED_ERROR_TYPE, positionalArguments)));
    }

    public ConnectionRefusedError(PythonLikeType type) {
        super(type);
    }

    public ConnectionRefusedError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ConnectionRefusedError(PythonLikeType type, String message) {
        super(type, message);
    }
}
