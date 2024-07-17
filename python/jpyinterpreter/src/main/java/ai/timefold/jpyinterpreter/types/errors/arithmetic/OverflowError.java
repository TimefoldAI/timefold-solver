package ai.timefold.jpyinterpreter.types.errors.arithmetic;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * The base class for those built-in exceptions that are raised for various arithmetic errors
 */
public class OverflowError extends ArithmeticError {
    final public static PythonLikeType OVERFLOW_ERROR_TYPE =
            new PythonLikeType("OverflowError", OverflowError.class, List.of(ARITHMETIC_ERROR_TYPE)),
            $TYPE = OVERFLOW_ERROR_TYPE;

    static {
        OVERFLOW_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new OverflowError(OVERFLOW_ERROR_TYPE,
                        positionalArguments)));
    }

    public OverflowError(PythonLikeType type) {
        super(type);
    }

    public OverflowError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public OverflowError(PythonLikeType type, String message) {
        super(type, message);
    }
}
