package ai.timefold.solver.python.score;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonBendableDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("BendableDecimalScore", PythonBendableDecimalScore.class);
    public PythonInteger init_score;
    public PythonLikeTuple<PythonDecimal> hard_scores;
    public PythonLikeTuple<PythonDecimal> soft_scores;

    public PythonBendableDecimalScore() {
        super(TYPE);
    }

    public static PythonBendableDecimalScore of(int[] hardScores, int[] softScores) {
        var out = new PythonBendableDecimalScore();
        out.init_score = PythonInteger.ZERO;
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }

    public static PythonBendableDecimalScore ofUninitialized(int initScore, int[] hardScores, int[] softScores) {
        var out = new PythonBendableDecimalScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }
}
