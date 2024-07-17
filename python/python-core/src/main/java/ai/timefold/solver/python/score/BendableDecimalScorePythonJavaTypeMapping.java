package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;

public final class BendableDecimalScorePythonJavaTypeMapping
        implements PythonJavaTypeMapping<PythonLikeObject, BendableBigDecimalScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field initScoreField;
    private final Field hardScoresField;
    private final Field softScoresField;

    public BendableDecimalScorePythonJavaTypeMapping(PythonLikeType type)
            throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        this.type = type;
        Class<?> clazz = type.getJavaClass();
        constructor = clazz.getConstructor();
        initScoreField = clazz.getField("init_score");
        hardScoresField = clazz.getField("hard_scores");
        softScoresField = clazz.getField("soft_scores");
    }

    @Override
    public PythonLikeType getPythonType() {
        return type;
    }

    @Override
    public Class<? extends BendableBigDecimalScore> getJavaType() {
        return BendableBigDecimalScore.class;
    }

    private static PythonLikeTuple<PythonDecimal> toPythonList(BigDecimal[] scores) {
        PythonLikeTuple<PythonDecimal> out = new PythonLikeTuple<>();
        for (var score : scores) {
            out.add(new PythonDecimal(score));
        }
        return out;
    }

    @Override
    public PythonLikeObject toPythonObject(BendableBigDecimalScore javaObject) {
        try {
            var instance = constructor.newInstance();
            initScoreField.set(instance, PythonInteger.valueOf(javaObject.initScore()));
            hardScoresField.set(instance, toPythonList(javaObject.hardScores()));
            softScoresField.set(instance, toPythonList(javaObject.softScores()));
            return (PythonLikeObject) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public BendableBigDecimalScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var initScore = ((PythonInteger) initScoreField.get(pythonObject)).value.intValue();
            var hardScoreTuple = ((PythonLikeTuple) hardScoresField.get(pythonObject));
            var softScoreTuple = ((PythonLikeTuple) softScoresField.get(pythonObject));
            BigDecimal[] hardScores = new BigDecimal[hardScoreTuple.size()];
            BigDecimal[] softScores = new BigDecimal[softScoreTuple.size()];
            for (int i = 0; i < hardScores.length; i++) {
                hardScores[i] = ((PythonDecimal) hardScoreTuple.get(i)).value;
            }
            for (int i = 0; i < softScores.length; i++) {
                softScores[i] = ((PythonDecimal) softScoreTuple.get(i)).value;
            }
            if (initScore == 0) {
                return BendableBigDecimalScore.of(hardScores, softScores);
            } else {
                return BendableBigDecimalScore.ofUninitialized(initScore, hardScores, softScores);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
