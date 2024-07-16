package ai.timefold.jpyinterpreter.types.errors.warning;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class EncodingWarning extends Warning {
    public final static PythonLikeType ENCODING_WARNING_TYPE =
            new PythonLikeType("EncodingWarning", EncodingWarning.class, List.of(WARNING_TYPE)),
            $TYPE = ENCODING_WARNING_TYPE;

    static {
        ENCODING_WARNING_TYPE.setConstructor(
                ((positionalArguments, namedArguments, callerInstance) -> new EncodingWarning(ENCODING_WARNING_TYPE,
                        positionalArguments)));
    }

    public EncodingWarning() {
        super(ENCODING_WARNING_TYPE);
    }

    public EncodingWarning(String message) {
        super(ENCODING_WARNING_TYPE, message);
    }

    public EncodingWarning(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public EncodingWarning(PythonLikeType type) {
        super(type);
    }

    public EncodingWarning(PythonLikeType type, String message) {
        super(type, message);
    }
}
