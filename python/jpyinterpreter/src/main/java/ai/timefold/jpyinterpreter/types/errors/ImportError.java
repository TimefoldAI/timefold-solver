package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ImportError extends PythonBaseException {
    final public static PythonLikeType IMPORT_ERROR_TYPE =
            new PythonLikeType("ImportError", ImportError.class, List.of(BASE_EXCEPTION_TYPE)),
            $TYPE = IMPORT_ERROR_TYPE;

    static {
        IMPORT_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ImportError(IMPORT_ERROR_TYPE,
                        positionalArguments)));
    }

    public ImportError(PythonLikeType type) {
        super(type);
    }

    public ImportError(PythonLikeType type, String message) {
        super(type, message);
    }

    public ImportError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
