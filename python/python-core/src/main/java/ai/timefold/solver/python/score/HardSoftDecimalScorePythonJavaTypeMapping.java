package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;

public final class HardSoftDecimalScorePythonJavaTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, HardSoftBigDecimalScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field initScoreField;
    private final Field hardScoreField;
    private final Field softScoreField;

    public HardSoftDecimalScorePythonJavaTypeMapping(PythonLikeType type)
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
    public Class<? extends HardSoftBigDecimalScore> getJavaType() {
        return HardSoftBigDecimalScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(HardSoftBigDecimalScore javaObject) {
        try {
            var instance = constructor.newInstance();
            initScoreField.set(instance, PythonInteger.valueOf(javaObject.initScore()));
            hardScoreField.set(instance, new PythonDecimal(javaObject.hardScore()));
            softScoreField.set(instance, new PythonDecimal(javaObject.softScore()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public HardSoftBigDecimalScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var initScore = ((PythonInteger) initScoreField.get(pythonObject)).value.intValue();
            var hardScore = ((PythonDecimal) hardScoreField.get(pythonObject)).value;
            var softScore = ((PythonDecimal) softScoreField.get(pythonObject)).value;
            if (initScore == 0) {
                return HardSoftBigDecimalScore.of(hardScore, softScore);
            } else {
                return HardSoftBigDecimalScore.ofUninitialized(initScore, hardScore, softScore);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
