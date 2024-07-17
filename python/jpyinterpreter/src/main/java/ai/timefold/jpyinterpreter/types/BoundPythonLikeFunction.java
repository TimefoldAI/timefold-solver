package ai.timefold.jpyinterpreter.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public class BoundPythonLikeFunction implements PythonLikeFunction {
    private final PythonLikeObject instance;
    private final PythonLikeFunction function;

    public BoundPythonLikeFunction(PythonLikeObject instance, PythonLikeFunction function) {
        this.instance = instance;
        this.function = function;
    }

    public static BoundPythonLikeFunction boundToTypeOfObject(PythonLikeObject instance, PythonLikeFunction function) {
        if (instance instanceof PythonLikeType) {
            return new BoundPythonLikeFunction(instance, function);
        } else {
            return new BoundPythonLikeFunction(instance.$getType(), function);
        }
    }

    public PythonLikeObject getInstance() {
        return instance;
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        ArrayList<PythonLikeObject> actualPositionalArgs = new ArrayList<>(positionalArguments.size() + 1);
        actualPositionalArgs.add(instance);
        actualPositionalArgs.addAll(positionalArguments);
        return function.$call(actualPositionalArgs, namedArguments, null);
    }
}
