package ai.timefold.jpyinterpreter.builtins;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.BoundPythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;

public class FunctionBuiltinOperations {
    public static PythonLikeObject bindFunctionToInstance(final PythonLikeFunction function, final PythonLikeObject instance,
            final PythonLikeType type) {
        if (instance == PythonNone.INSTANCE) {
            return function;
        }
        return new BoundPythonLikeFunction(instance, function);
    }

    public static PythonLikeObject bindFunctionToType(final PythonLikeFunction function, final PythonLikeObject instance,
            final PythonLikeType type) {
        return new BoundPythonLikeFunction(type, function);
    }
}
