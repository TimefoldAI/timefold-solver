package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Python class for general exceptions. Equivalent to Java's {@link RuntimeException}
 */
public class PythonException extends PythonBaseException {
    final public static PythonLikeType EXCEPTION_TYPE =
            new PythonLikeType("Exception", PythonException.class, List.of(BASE_EXCEPTION_TYPE)),
            $TYPE = EXCEPTION_TYPE;

    static {
        EXCEPTION_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new PythonException(EXCEPTION_TYPE,
                        positionalArguments)));
    }

    public PythonException(PythonLikeType type) {
        super(type);
    }

    public PythonException(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public PythonException(PythonLikeType type, String message) {
        super(type, message);
    }
}
