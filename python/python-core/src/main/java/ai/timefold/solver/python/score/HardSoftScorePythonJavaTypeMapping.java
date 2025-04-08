package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

public final class HardSoftScorePythonJavaTypeMapping implements PythonJavaTypeMapping<PythonLikeObject, HardSoftLongScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field hardScoreField;
    private final Field softScoreField;

    public HardSoftScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        var clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        hardScoreField = clazz.getField("hard_score");
        softScoreField = clazz.getField("soft_score");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    public Class<? extends HardSoftLongScore> getJavaType() {
        return HardSoftLongScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(HardSoftLongScore javaObject) {
        try {
            var instance = constructor.newInstance();
            hardScoreField.set(instance, PythonInteger.valueOf(javaObject.hardScore()));
            softScoreField.set(instance, PythonInteger.valueOf(javaObject.softScore()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public HardSoftLongScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var hardScore = ((PythonInteger) hardScoreField.get(pythonObject)).value.longValue();
            var softScore = ((PythonInteger) softScoreField.get(pythonObject)).value.longValue();
            return HardSoftLongScore.of(hardScore, softScore);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
