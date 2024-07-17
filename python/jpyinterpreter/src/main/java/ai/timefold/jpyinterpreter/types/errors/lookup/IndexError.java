package ai.timefold.jpyinterpreter.types.errors.lookup;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * The base class for the exceptions that are raised when a key or index used on a mapping or sequence is invalid.
 */
public class IndexError extends LookupError {
    final public static PythonLikeType INDEX_ERROR_TYPE =
            new PythonLikeType("IndexError", IndexError.class, List.of(LOOKUP_ERROR_TYPE)),
            $TYPE = INDEX_ERROR_TYPE;

    static {
        INDEX_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new IndexError(INDEX_ERROR_TYPE,
                        positionalArguments)));
    }

    public IndexError(PythonLikeType type) {
        super(type);
    }

    public IndexError(String message) {
        super(INDEX_ERROR_TYPE, message);
    }

    public IndexError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public IndexError(PythonLikeType type, String message) {
        super(type, message);
    }
}
