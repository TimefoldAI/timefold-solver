package ai.timefold.jpyinterpreter.types.errors.syntax;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class TabError extends IndentationError {
    final public static PythonLikeType TAB_ERROR_TYPE =
            new PythonLikeType("TabError", TabError.class, List.of(INDENTATION_ERROR_TYPE)),
            $TYPE = TAB_ERROR_TYPE;

    static {
        TAB_ERROR_TYPE
                .setConstructor(((positionalArguments, namedArguments, callerInstance) -> new TabError(TAB_ERROR_TYPE,
                        positionalArguments)));
    }

    public TabError(PythonLikeType type) {
        super(type);
    }

    public TabError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public TabError(PythonLikeType type, String message) {
        super(type, message);
    }
}
