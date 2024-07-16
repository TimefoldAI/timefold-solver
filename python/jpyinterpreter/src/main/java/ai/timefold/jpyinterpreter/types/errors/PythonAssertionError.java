package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class PythonAssertionError extends PythonException {
    public static final PythonLikeType ASSERTION_ERROR_TYPE = new PythonLikeType("AssertionError",
            PythonAssertionError.class,
            List.of(EXCEPTION_TYPE)),
            $TYPE = ASSERTION_ERROR_TYPE;

    static {
        ASSERTION_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new PythonAssertionError(ASSERTION_ERROR_TYPE,
                        positionalArguments)));
    }

    public PythonAssertionError() {
        super(ASSERTION_ERROR_TYPE);
    }

    public PythonAssertionError(PythonLikeType type) {
        super(type);
    }

    public PythonAssertionError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }
}
