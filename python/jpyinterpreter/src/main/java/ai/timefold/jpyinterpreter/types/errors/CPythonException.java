package ai.timefold.jpyinterpreter.types.errors;

import java.util.List;

import ai.timefold.jpyinterpreter.types.PythonLikeType;

/**
 * Python class for general exceptions. Equivalent to Java's {@link RuntimeException}
 */
public class CPythonException extends PythonException {
    public final static PythonLikeType CPYTHON_EXCEPTION_TYPE =
            new PythonLikeType("CPython", CPythonException.class, List.of(EXCEPTION_TYPE)),
            $TYPE = CPYTHON_EXCEPTION_TYPE;

    public CPythonException() {
        super(CPYTHON_EXCEPTION_TYPE);
    }

    public CPythonException(String message) {
        super(CPYTHON_EXCEPTION_TYPE, message);
    }
}
