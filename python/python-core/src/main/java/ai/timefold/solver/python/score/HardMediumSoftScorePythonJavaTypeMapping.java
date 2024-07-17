package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

public final class HardMediumSoftScorePythonJavaTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, HardMediumSoftLongScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field initScoreField;
    private final Field hardScoreField;
    private final Field mediumScoreField;
    private final Field softScoreField;

    public HardMediumSoftScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        Class<?> clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        initScoreField = clazz.getField("init_score");
        hardScoreField = clazz.getField("hard_score");
        mediumScoreField = clazz.getField("medium_score");
        softScoreField = clazz.getField("soft_score");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    public Class<? extends HardMediumSoftLongScore> getJavaType() {
        return HardMediumSoftLongScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(HardMediumSoftLongScore javaObject) {
        try {
            var instance = constructor.newInstance();
            initScoreField.set(instance, PythonInteger.valueOf(javaObject.initScore()));
            hardScoreField.set(instance, PythonInteger.valueOf(javaObject.hardScore()));
            mediumScoreField.set(instance, PythonInteger.valueOf(javaObject.mediumScore()));
            softScoreField.set(instance, PythonInteger.valueOf(javaObject.softScore()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public HardMediumSoftLongScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var initScore = ((PythonInteger) initScoreField.get(pythonObject)).value.intValue();
            var hardScore = ((PythonInteger) hardScoreField.get(pythonObject)).value.longValue();
            var mediumScore = ((PythonInteger) mediumScoreField.get(pythonObject)).value.longValue();
            var softScore = ((PythonInteger) softScoreField.get(pythonObject)).value.longValue();
            if (initScore == 0) {
                return HardMediumSoftLongScore.of(hardScore, mediumScore, softScore);
            } else {
                return HardMediumSoftLongScore.ofUninitialized(initScore, hardScore, mediumScore, softScore);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
