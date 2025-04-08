package ai.timefold.solver.python.score;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;

public class PythonBendableDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("BendableDecimalScore", PythonBendableDecimalScore.class);
    public PythonLikeTuple<PythonDecimal> hard_scores;
    public PythonLikeTuple<PythonDecimal> soft_scores;

    public PythonBendableDecimalScore() {
        super(TYPE);
    }

    public static PythonBendableDecimalScore of(int[] hardScores, int[] softScores) {
        var out = new PythonBendableDecimalScore();
        out.hard_scores = IntStream.of(hardScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        out.soft_scores = IntStream.of(softScores)
                .mapToObj(i -> new PythonDecimal(BigDecimal.valueOf(i)))
                .collect(Collectors.toCollection(PythonLikeTuple::new));
        return out;
    }

}
