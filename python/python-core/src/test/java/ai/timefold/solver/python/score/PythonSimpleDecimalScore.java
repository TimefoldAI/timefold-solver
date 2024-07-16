package ai.timefold.solver.python.score;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonSimpleDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("SimpleDecimalScore", PythonSimpleDecimalScore.class);
    public PythonInteger init_score;
    public PythonDecimal score;

    public PythonSimpleDecimalScore() {
        super(TYPE);
    }

    public static PythonSimpleDecimalScore of(int score) {
        var out = new PythonSimpleDecimalScore();
        out.init_score = PythonInteger.ZERO;
        out.score = new PythonDecimal(BigDecimal.valueOf(score));
        return out;
    }

    public static PythonSimpleDecimalScore ofUninitialized(int initScore, int score) {
        var out = new PythonSimpleDecimalScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.score = new PythonDecimal(BigDecimal.valueOf(score));
        return out;
    }
}
