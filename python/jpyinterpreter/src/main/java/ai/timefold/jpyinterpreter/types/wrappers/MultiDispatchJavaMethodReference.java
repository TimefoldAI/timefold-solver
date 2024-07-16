package ai.timefold.jpyinterpreter.types.wrappers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.TypeError;

public class MultiDispatchJavaMethodReference implements PythonLikeFunction {
    private final List<Method> methodList;

    public MultiDispatchJavaMethodReference() {
        this.methodList = new ArrayList<>();
    }

    public void addMethod(Method method) {
        methodList.add(method);
    }

    public Method getNoArgsMethod() {
        for (Method method : methodList) {
            if (method.getParameterCount() == 0) {
                return method;
            }
        }
        throw new TypeError();
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        Object self;
        Object[] args;
        for (Method method : methodList) {
            if (Modifier.isStatic(method.getModifiers())) {
                if (method.getParameterCount() != positionalArguments.size()) {
                    continue;
                }
                self = null;
                try {
                    args = unwrapPrimitiveArguments(method, positionalArguments);
                } catch (TypeError e) {
                    continue;
                }

            } else {
                if (method.getParameterCount() + 1 != positionalArguments.size()) {
                    continue;
                }
                self = positionalArguments.get(0);
                if (self instanceof JavaObjectWrapper) { // unwrap wrapped Java Objects
                    self = ((JavaObjectWrapper) self).getWrappedObject();
                }
                try {
                    args = unwrapPrimitiveArguments(method, positionalArguments.subList(1, positionalArguments.size()));
                } catch (TypeError e) {
                    continue;
                }
            }
            try {
                return JavaPythonTypeConversionImplementor.wrapJavaObject(method.invoke(self, args));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Method (" + method + ") is not accessible.", e);
            } catch (InvocationTargetException e) {
                throw (RuntimeException) e.getCause();
            }
        }
        throw new TypeError("No method with matching signature found for %s in method list %s.".formatted(positionalArguments,
                methodList));
    }

    private Object[] unwrapPrimitiveArguments(Method method, List<PythonLikeObject> positionalArguments) {
        Object[] out = new Object[method.getParameterCount()];
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < positionalArguments.size(); i++) {
            PythonLikeObject argument = positionalArguments.get(i);
            out[i] = JavaPythonTypeConversionImplementor.convertPythonObjectToJavaType(parameterTypes[i], argument);
        }

        return out;
    }

    @Override
    public PythonLikeType $getType() {
        if (Modifier.isStatic(methodList.get(0).getModifiers())) {
            return BuiltinTypes.STATIC_FUNCTION_TYPE;
        } else {
            return BuiltinTypes.FUNCTION_TYPE;
        }
    }
}
