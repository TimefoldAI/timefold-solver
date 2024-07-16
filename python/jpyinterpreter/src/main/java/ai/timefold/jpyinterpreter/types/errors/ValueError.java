package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class ValueError extends PythonException {
    public final static PythonLikeType VALUE_ERROR_TYPE =
            new PythonLikeType("ValueError", ValueError.class, List.of(EXCEPTION_TYPE)),
            $TYPE = VALUE_ERROR_TYPE;

    static {
        VALUE_ERROR_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new ValueError(VALUE_ERROR_TYPE,
                        positionalArguments)));
    }

    public ValueError() {
        super(VALUE_ERROR_TYPE);
    }

    public ValueError(String message) {
        super(VALUE_ERROR_TYPE, message);
    }

    public ValueError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public ValueError(PythonLikeType type) {
        super(type);
    }

    public ValueError(PythonLikeType type, String message) {
        super(type, message);
    }
}
