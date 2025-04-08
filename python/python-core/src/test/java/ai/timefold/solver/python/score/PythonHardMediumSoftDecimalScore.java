package ai.timefold.solver.python.score;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;

public class PythonHardMediumSoftDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE =
            new PythonLikeType("HardMediumSoftDecimalScore", PythonHardMediumSoftDecimalScore.class);
    public PythonDecimal hard_score;
    public PythonDecimal medium_score;
    public PythonDecimal soft_score;

    public PythonHardMediumSoftDecimalScore() {
        super(TYPE);
    }

    public static PythonHardMediumSoftDecimalScore of(int hardScore, int mediumScore, int softScore) {
        var out = new PythonHardMediumSoftDecimalScore();
        out.hard_score = new PythonDecimal(BigDecimal.valueOf(hardScore));
        out.medium_score = new PythonDecimal(BigDecimal.valueOf(mediumScore));
        out.soft_score = new PythonDecimal(BigDecimal.valueOf(softScore));
        return out;
    }

}
