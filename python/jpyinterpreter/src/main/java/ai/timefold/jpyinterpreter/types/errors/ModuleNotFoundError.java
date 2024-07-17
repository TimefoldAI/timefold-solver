package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class ModuleNotFoundError extends ImportError {
    final public static PythonLikeType MODULE_NOT_FOUND_ERROR_TYPE =
            new PythonLikeType("ModuleNotFoundError", ModuleNotFoundError.class, List.of(IMPORT_ERROR_TYPE)),
            $TYPE = MODULE_NOT_FOUND_ERROR_TYPE;

    static {
        MODULE_NOT_FOUND_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new ModuleNotFoundError(MODULE_NOT_FOUND_ERROR_TYPE, positionalArguments)));
    }

    public ModuleNotFoundError(PythonLikeType type) {
        super(type);
    }

    public ModuleNotFoundError(PythonLikeType type, String message) {
        super(type, message);
    }

    public ModuleNotFoundError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
