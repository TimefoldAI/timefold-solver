package ai.timefold.jpyinterpreter.types.errors.lookup;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * The base class for the exceptions that are raised when a key or index used on a mapping or sequence is invalid.
 */
public class KeyError extends LookupError {
    final public static PythonLikeType KEY_ERROR_TYPE =
            new PythonLikeType("KeyError", KeyError.class, List.of(LOOKUP_ERROR_TYPE)),
            $TYPE = KEY_ERROR_TYPE;

    static {
        KEY_ERROR_TYPE
                .setConstructor(((positionalArguments, namedArguments, callerInstance) -> new KeyError(KEY_ERROR_TYPE,
                        positionalArguments)));
    }

    public KeyError(PythonLikeType type) {
        super(type);
    }

    public KeyError(String message) {
        super(KEY_ERROR_TYPE, message);
    }

    public KeyError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public KeyError(PythonLikeType type, String message) {
        super(type, message);
    }
}
