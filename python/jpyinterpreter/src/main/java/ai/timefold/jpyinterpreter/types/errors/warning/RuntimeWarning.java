package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class RuntimeWarning extends Warning {
    public final static PythonLikeType RUNTIME_WARNING_TYPE =
            new PythonLikeType("RuntimeWarning", RuntimeWarning.class, List.of(WARNING_TYPE)),
            $TYPE = RUNTIME_WARNING_TYPE;

    static {
        RUNTIME_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new RuntimeWarning(RUNTIME_WARNING_TYPE,
                        positionalArguments)));
    }

    public RuntimeWarning() {
        super(RUNTIME_WARNING_TYPE);
    }

    public RuntimeWarning(String message) {
        super(RUNTIME_WARNING_TYPE, message);
    }

    public RuntimeWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public RuntimeWarning(PythonLikeType type) {
        super(type);
    }

    public RuntimeWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
