package ai.timefold.solver.python.score;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonBendableScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("BendableScore", PythonBendableScore.class);
    public PythonInteger init_score;
    public PythonLikeTuple<PythonInteger> hard_scores;
    public PythonLikeTuple<PythonInteger> soft_scores;

    public PythonBendableScore() {
        super(TYPE);
    }

    public static PythonBendableScore of(int[] hardScores, int[] softScores) {
        var out = new PythonBendableScore();
        out.init_score = PythonInteger.ZERO;
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }

    public static PythonBendableScore ofUninitialized(int initScore, int[] hardScores, int[] softScores) {
        var out = new PythonBendableScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }
}
