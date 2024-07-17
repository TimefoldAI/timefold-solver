package ai.timefold.jpyinterpreter.types.errors.arithmetic;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * The base class for those built-in exceptions that are raised for various arithmetic errors
 */
public class FloatingPointError extends ArithmeticError {
    final public static PythonLikeType FLOATING_POINT_ERROR_TYPE =
            new PythonLikeType("FloatingPointError", FloatingPointError.class, List.of(ARITHMETIC_ERROR_TYPE)),
            $TYPE = FLOATING_POINT_ERROR_TYPE;

    static {
        FLOATING_POINT_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new FloatingPointError(FLOATING_POINT_ERROR_TYPE, positionalArguments)));
    }

    public FloatingPointError(PythonLikeType type) {
        super(type);
    }

    public FloatingPointError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public FloatingPointError(PythonLikeType type, String message) {
        super(type, message);
    }
}
