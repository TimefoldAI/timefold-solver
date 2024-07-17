package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class UnboundLocalError extends NameError {
    public final static PythonLikeType UNBOUND_LOCAL_ERROR_TYPE =
            new PythonLikeType("UnboundLocalError", UnboundLocalError.class, List.of(NAME_ERROR_TYPE)),
            $TYPE = UNBOUND_LOCAL_ERROR_TYPE;

    static {
        UNBOUND_LOCAL_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new UnboundLocalError(UNBOUND_LOCAL_ERROR_TYPE, positionalArguments)));
    }

    public UnboundLocalError() {
        super(UNBOUND_LOCAL_ERROR_TYPE);
    }

    public UnboundLocalError(String message) {
        super(UNBOUND_LOCAL_ERROR_TYPE, message);
    }

    public UnboundLocalError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public UnboundLocalError(PythonLikeType type) {
        super(type);
    }

    public UnboundLocalError(PythonLikeType type, String message) {
        super(type, message);
    }
}
