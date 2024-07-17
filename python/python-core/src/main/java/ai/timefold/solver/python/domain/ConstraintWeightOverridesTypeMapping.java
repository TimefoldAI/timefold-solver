package ai.timefold.solver.python.domain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.wrappers.JavaObjectWrapper;
import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;

@SuppressWarnings("rawtypes")
public class ConstraintWeightOverridesTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, ConstraintWeightOverrides> {

    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field delegateField;

    public ConstraintWeightOverridesTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        this.type = type;
        Class<?> clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        delegateField = clazz.getField("_delegate");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<? extends ConstraintWeightOverrides> getJavaType() {
        return ConstraintWeightOverrides.class;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public PythonLikeObject toPythonObject(ConstraintWeightOverrides javaObject) {
        try {
            PythonLikeObject instance = (PythonLikeObject) constructor.newInstance();
            delegateField.set(instance, new JavaObjectWrapper(javaObject));
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ConstraintWeightOverrides<?> toJavaObject(PythonLikeObject pythonObject) {
        try {
            JavaObjectWrapper out = (JavaObjectWrapper) delegateField.get(pythonObject);
            return (ConstraintWeightOverrides<?>) out.getWrappedObject();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
