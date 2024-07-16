package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.errors.PythonException;

public class Warning extends PythonException {
    public final static PythonLikeType WARNING_TYPE =
            new PythonLikeType("Warning", Warning.class, List.of(PythonException.EXCEPTION_TYPE)),
            $TYPE = WARNING_TYPE;

    static {
        WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new Warning(WARNING_TYPE, positionalArguments)));
    }

    public Warning() {
        super(WARNING_TYPE);
    }

    public Warning(String message) {
        super(WARNING_TYPE, message);
    }

    public Warning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public Warning(PythonLikeType type) {
        super(type);
    }

    public Warning(PythonLikeType type, String message) {
        super(type, message);
    }
}
