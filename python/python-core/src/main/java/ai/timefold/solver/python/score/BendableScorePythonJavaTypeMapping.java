package ai.timefold.solver.python.score;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonJavaTypeMapping;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;

public final class BendableScorePythonJavaTypeMapping implements PythonJavaTypeMapping<PythonLikeObject, BendableLongScore> {
    private final PythonLikeType type;
    private final Constructor<?> constructor;
    private final Field initScoreField;
    private final Field hardScoresField;
    private final Field softScoresField;

    public BendableScorePythonJavaTypeMapping(PythonLikeType type)
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
    public Class<? extends BendableLongScore> getJavaType() {
        return BendableLongScore.class;
    }

    private static PythonLikeTuple<PythonInteger> toPythonList(long[] scores) {
        PythonLikeTuple<PythonInteger> out = new PythonLikeTuple<>();
        for (long score : scores) {
            out.add(PythonInteger.valueOf(score));
        }
        return out;
    }

    @Override
    public PythonLikeObject toPythonObject(BendableLongScore javaObject) {
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
    public BendableLongScore toJavaObject(PythonLikeObject pythonObject) {
        try {
            var initScore = ((PythonInteger) initScoreField.get(pythonObject)).value.intValue();
            var hardScoreTuple = ((PythonLikeTuple) hardScoresField.get(pythonObject));
            var softScoreTuple = ((PythonLikeTuple) softScoresField.get(pythonObject));
            long[] hardScores = new long[hardScoreTuple.size()];
            long[] softScores = new long[softScoreTuple.size()];
            for (int i = 0; i < hardScores.length; i++) {
                hardScores[i] = ((PythonInteger) hardScoreTuple.get(i)).value.longValue();
            }
            for (int i = 0; i < softScores.length; i++) {
                softScores[i] = ((PythonInteger) softScoreTuple.get(i)).value.longValue();
            }
            if (initScore == 0) {
                return BendableLongScore.of(hardScores, softScores);
            } else {
                return BendableLongScore.ofUninitialized(initScore, hardScores, softScores);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
