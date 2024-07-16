package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class TypeError extends PythonException {
    public final static PythonLikeType TYPE_ERROR_TYPE =
            new PythonLikeType("TypeError", TypeError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = TYPE_ERROR_TYPE;

    static {
        TYPE_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new TypeError(TYPE_ERROR_TYPE, positionalArguments)));
    }

    public TypeError() {
        super(TYPE_ERROR_TYPE);
    }

    public TypeError(String message) {
        super(TYPE_ERROR_TYPE, message);
    }

    public TypeError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public TypeError(PythonLikeType type) {
        super(type);
    }

    public TypeError(PythonLikeType type, String message) {
        super(type, message);
    }
}
