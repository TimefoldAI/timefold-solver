package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class UserWarning extends Warning {
    public final static PythonLikeType USER_WARNING_TYPE =
            new PythonLikeType("UserWarning", UserWarning.class, List.of(WARNING_TYPE)),
            $TYPE = USER_WARNING_TYPE;

    static {
        USER_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new UserWarning(USER_WARNING_TYPE,
                        positionalArguments)));
    }

    public UserWarning() {
        super(USER_WARNING_TYPE);
    }

    public UserWarning(String message) {
        super(USER_WARNING_TYPE, message);
    }

    public UserWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public UserWarning(PythonLikeType type) {
        super(type);
    }

    public UserWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
