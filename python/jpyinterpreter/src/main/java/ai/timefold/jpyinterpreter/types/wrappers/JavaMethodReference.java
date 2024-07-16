package ai.timefold.jpyinterpreter.types.wrappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;

public class JavaMethodReference implements PythonLikeFunction {
    private final Method method;
    private final Map<String, Integer> parameterNameToIndexMap;

    public JavaMethodReference(Method method, Map<String, Integer> parameterNameToIndexMap) {
        this.method = method;
        this.parameterNameToIndexMap = parameterNameToIndexMap;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        Object self;
        Object[] args;
        if (Modifier.isStatic(method.getModifiers())) {
            self = null;
            args = unwrapPrimitiveArguments(positionalArguments, namedArguments);
        } else {
            self = positionalArguments.get(0);
            if (self instanceof JavaObjectWrapper) { // unwrap wrapped Java Objects
                self = ((JavaObjectWrapper) self).getWrappedObject();
            }
            args = unwrapPrimitiveArguments(positionalArguments.subList(1, positionalArguments.size()), namedArguments);
        }
        try {
            return JavaPythonTypeConversionImplementor.wrapJavaObject(method.invoke(self, args));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    private Object[] unwrapPrimitiveArguments(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments) {
        namedArguments = (namedArguments != null) ? namedArguments : Map.of();
        Object[] out = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[i], argument);
        }

        for (PythonString key : namedArguments.keySet()) {
            int index = parameterNameToIndexMap.get(key.value);
            PythonLikeObject argument = namedArguments.get(key);
            out[index] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[index], argument);
        }

        return out;
    }

    @Override
    public PythonLikeType $getType() {
        if (Modifier.isStatic(method.getModifiers())) {
            return BuiltinTypes.STATIC_FUNCTION_TYPE;
        } else {
            return BuiltinTypes.FUNCTION_TYPE;
        }
    }
}
