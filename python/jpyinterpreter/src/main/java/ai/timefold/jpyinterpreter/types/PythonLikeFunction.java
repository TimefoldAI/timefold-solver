package ai.timefold.jpyinterpreter.types;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public interface PythonLikeFunction extends PythonLikeObject {
    static PythonLikeType getStaticFunctionType() {
        return BuiltinTypes.STATIC_FUNCTION_TYPE;
    }

    static PythonLikeType getFunctionType() {
        return BuiltinTypes.FUNCTION_TYPE;
    }

    static PythonLikeType getClassFunctionType() {
        return BuiltinTypes.CLASS_FUNCTION_TYPE;
    }

    /**
     * Calls the function with positional arguments and named arguments.
     *
     * @param positionalArguments Positional arguments
     * @param namedArguments Named arguments
     * @param callerInstance The first argument passed to the caller, if any; null otherwise (used for super)
     * @return The function result
     */
    PythonLikeObject $call(List<PythonLikeObject> positionalArguments, Map<PythonString, PythonLikeObject> namedArguments,
            PythonLikeObject callerInstance);

    @Override
    default PythonLikeObject $getAttributeOrNull(String attributeName) {
        return null;
    }

    @Override
    default void $setAttribute(String attributeName, PythonLikeObject value) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void $deleteAttribute(String attributeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    default PythonLikeType $getType() {
        return BuiltinTypes.FUNCTION_TYPE;
    }
}
