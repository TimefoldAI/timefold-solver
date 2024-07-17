package ai.timefold.jpyinterpreter.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public class GeneratedFunctionMethodReference implements PythonLikeFunction {

    private final Object instance;

    private final Method method;
    private final Map<String, Integer> parameterNameToIndexMap;
    private final PythonLikeType type;

    public GeneratedFunctionMethodReference(Object instance, Method method, Map<String, Integer> parameterNameToIndexMap,
            PythonLikeType type) {
        this.instance = instance;
        this.method = method;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
        this.type = type;
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        Object[] args = unwrapPrimitiveArguments(positionalArguments, namedArguments);
        try {
            return (PythonLikeObject) method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] unwrapPrimitiveArguments(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();
        Object[] out = new Object[method.getParameterCount()];

        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = argument;
        }

        for (PythonString key : namedArguments.keySet()) {
            int index = parameterNameToIndexMap.get(key.value);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = argument;
        }

        return out;
    }

    @Override
    public PythonLikeType $getType() {
        return type;
    }
}
