package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;

public final class SimpleScorePythonJavaTypeMapping implements PythonJavaTypeMapping<PythonLikeObject, SimpleLongScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field scoreField;

    public SimpleScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        var clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        scoreField = clazz.getField("score");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    public Class<? extends SimpleLongScore> getJavaType() {
        return SimpleLongScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(SimpleLongScore javaObject) {
        try {
            var instance = constructor.newInstance();
            scoreField.set(instance, PythonInteger.valueOf(javaObject.score()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SimpleLongScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var score = ((PythonInteger) scoreField.get(pythonObject)).value.longValue();
            return SimpleLongScore.of(score);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
