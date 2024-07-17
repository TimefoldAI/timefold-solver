package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class DeprecationWarning extends Warning {
    public final static PythonLikeType DEPRECATION_WARNING_TYPE =
            new PythonLikeType("DeprecationWarning", DeprecationWarning.class, List.of(WARNING_TYPE)),
            $TYPE = DEPRECATION_WARNING_TYPE;

    static {
        DEPRECATION_WARNING_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new DeprecationWarning(DEPRECATION_WARNING_TYPE, positionalArguments)));
    }

    public DeprecationWarning() {
        super(DEPRECATION_WARNING_TYPE);
    }

    public DeprecationWarning(String message) {
        super(DEPRECATION_WARNING_TYPE, message);
    }

    public DeprecationWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public DeprecationWarning(PythonLikeType type) {
        super(type);
    }

    public DeprecationWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
