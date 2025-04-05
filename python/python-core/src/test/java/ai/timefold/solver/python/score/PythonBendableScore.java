package ai.timefold.solver.python.score;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonBendableScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("BendableScore", PythonBendableScore.class);
    public PythonLikeTuple<PythonInteger> hard_scores;
    public PythonLikeTuple<PythonInteger> soft_scores;

    public PythonBendableScore() {
        super(TYPE);
    }

    public static PythonBendableScore of(int[] hardScores, int[] softScores) {
        var out = new PythonBendableScore();
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(PythonInteger::valueOf)
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }

}
