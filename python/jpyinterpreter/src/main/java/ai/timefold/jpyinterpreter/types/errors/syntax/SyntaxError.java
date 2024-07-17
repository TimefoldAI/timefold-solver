package ai.timefold.jpyinterpreter.types.errors.syntax;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonException;

/**
 * Raised when a buffer related operation cannot be performed.
 */
public class SyntaxError extends PythonException {
    final public static PythonLikeType SYNTAX_ERROR_TYPE =
            new PythonLikeType("SyntaxError", SyntaxError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = SYNTAX_ERROR_TYPE;

    static {
        SYNTAX_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new SyntaxError(SYNTAX_ERROR_TYPE,
                        positionalArguments)));
    }

    public SyntaxError(PythonLikeType type) {
        super(type);
    }

    public SyntaxError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public SyntaxError(PythonLikeType type, String message) {
        super(type, message);
    }
}
