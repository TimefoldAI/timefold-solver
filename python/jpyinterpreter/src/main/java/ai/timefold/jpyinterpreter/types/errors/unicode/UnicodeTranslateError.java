package ai.timefold.jpyinterpreter.types.errors.unicode;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class UnicodeTranslateError extends UnicodeError {
    public final static PythonLikeType UNICODE_TRANSLATE_ERROR_TYPE =
            new PythonLikeType("UnicodeTranslateError", UnicodeTranslateError.class, List.of(UNICODE_ERROR_TYPE)),
            $TYPE = UNICODE_TRANSLATE_ERROR_TYPE;

    static {
        UNICODE_TRANSLATE_ERROR_TYPE.setConstructor(((positionalArguments,
                namedArguments,
                callerInstance) -> new UnicodeTranslateError(UNICODE_TRANSLATE_ERROR_TYPE, positionalArguments)));
    }

    public UnicodeTranslateError() {
        super(UNICODE_TRANSLATE_ERROR_TYPE);
    }

    public UnicodeTranslateError(String message) {
        super(UNICODE_TRANSLATE_ERROR_TYPE, message);
    }

    public UnicodeTranslateError(PythonLikeType type, List<PythonLikeObject> args) {
        super(type, args);
    }

    public UnicodeTranslateError(PythonLikeType type) {
        super(type);
    }

    public UnicodeTranslateError(PythonLikeType type, String message) {
        super(type, message);
    }
}
