package ai.timefold.solver.python.score;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonHardSoftDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("HardSoftDecimalScore", PythonHardSoftDecimalScore.class);
    public PythonInteger init_score;
    public PythonDecimal hard_score;
    public PythonDecimal soft_score;

    public PythonHardSoftDecimalScore() {
        super(TYPE);
    }

    public static PythonHardSoftDecimalScore of(int hardScore, int softScore) {
        var out = new PythonHardSoftDecimalScore();
        out.init_score = PythonInteger.ZERO;
        out.hard_score = new PythonDecimal(BigDecimal.valueOf(hardScore));
        out.soft_score = new PythonDecimal(BigDecimal.valueOf(softScore));
        return out;
    }

    public static PythonHardSoftDecimalScore ofUninitialized(int initScore, int hardScore, int softScore) {
        var out = new PythonHardSoftDecimalScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.hard_score = new PythonDecimal(BigDecimal.valueOf(hardScore));
        out.soft_score = new PythonDecimal(BigDecimal.valueOf(softScore));
        return out;
    }
}
