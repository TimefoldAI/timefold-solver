package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;

public final class HardMediumSoftDecimalScorePythonJavaTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, HardMediumSoftBigDecimalScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field hardScoreField;
    private final Field mediumScoreField;
    private final Field softScoreField;

    public HardMediumSoftDecimalScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        var clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        hardScoreField = clazz.getField("hard_score");
        mediumScoreField = clazz.getField("medium_score");
        softScoreField = clazz.getField("soft_score");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    public Class<? extends HardMediumSoftBigDecimalScore> getJavaType() {
        return HardMediumSoftBigDecimalScore.class;
    }

    @Override
    public PythonLikeObject toPythonObject(HardMediumSoftBigDecimalScore javaObject) {
        try {
            var instance = constructor.newInstance();
            hardScoreField.set(instance, new PythonDecimal(javaObject.hardScore()));
            mediumScoreField.set(instance, new PythonDecimal(javaObject.mediumScore()));
            softScoreField.set(instance, new PythonDecimal(javaObject.softScore()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public HardMediumSoftBigDecimalScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var hardScore = ((PythonDecimal) hardScoreField.get(pythonObject)).value;
            var mediumScore = ((PythonDecimal) mediumScoreField.get(pythonObject)).value;
            var softScore = ((PythonDecimal) softScoreField.get(pythonObject)).value;
            return HardMediumSoftBigDecimalScore.of(hardScore, mediumScore, softScore);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
