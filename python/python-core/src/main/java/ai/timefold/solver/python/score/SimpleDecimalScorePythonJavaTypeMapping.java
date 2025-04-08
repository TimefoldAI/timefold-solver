package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

public final class SimpleDecimalScorePythonJavaTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, SimpleBigDecimalScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field scoreField;

    public SimpleDecimalScorePythonJavaTypeMapping(PythonLikeType type)
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
    public Class<? extends SimpleBigDecimalScore> getJavaType() {
        return SimpleBigDecimalScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(SimpleBigDecimalScore javaObject) {
        try {
            var instance = constructor.newInstance();
            scoreField.set(instance, new PythonDecimal(javaObject.score()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public SimpleBigDecimalScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var score = ((PythonDecimal) scoreField.get(pythonObject)).value;
            return SimpleBigDecimalScore.of(score);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
