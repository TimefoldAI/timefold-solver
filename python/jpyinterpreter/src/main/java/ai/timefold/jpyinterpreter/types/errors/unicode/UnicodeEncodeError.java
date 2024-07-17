package ai.timefold.jpyinterpreter.types.errors.unicode;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class UnicodeEncodeError extends UnicodeError {
    public final static PythonLikeType UNICODE_ENCODE_ERROR_TYPE =
            new PythonLikeType("UnicodeEncodeError", UnicodeEncodeError.class, List.of(UNICODE_ERROR_TYPE)),
            $TYPE = UNICODE_ENCODE_ERROR_TYPE;

    static {
        UNICODE_ENCODE_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments, callerInstance) -> new UnicodeEncodeError(UNICODE_ENCODE_ERROR_TYPE, positionalArguments)));
    }

    public UnicodeEncodeError() {
        super(UNICODE_ENCODE_ERROR_TYPE);
    }

    public UnicodeEncodeError(String message) {
        super(UNICODE_ENCODE_ERROR_TYPE, message);
    }

    public UnicodeEncodeError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public UnicodeEncodeError(PythonLikeType type) {
        super(type);
    }

    public UnicodeEncodeError(PythonLikeType type, String message) {
        super(type, message);
    }
}
