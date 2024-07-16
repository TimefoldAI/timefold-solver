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
    private final Field initScoreField;
    private final Field hardScoreField;
    private final Field softScoreField;

    public HardSoftScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        Class<?> clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        initScoreField = clazz.getField("init_score");
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
            initScoreField.set(instance, PythonInteger.valueOf(javaObject.initScore()));
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
            var initScore = ((PythonInteger) initScoreField.get(pythonObject)).value.intValue();
            var hardScore = ((PythonInteger) hardScoreField.get(pythonObject)).value.longValue();
            var softScore = ((PythonInteger) softScoreField.get(pythonObject)).value.longValue();
            if (initScore == 0) {
                return HardSoftLongScore.of(hardScore, softScore);
            } else {
                return HardSoftLongScore.ofUninitialized(initScore, hardScore, softScore);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
