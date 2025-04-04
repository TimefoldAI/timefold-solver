package ai.timefold.solver.python.score;

import java.math.BigDecimal;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;

public class PythonSimpleDecimalScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("SimpleDecimalScore", PythonSimpleDecimalScore.class);
    public PythonDecimal score;

    public PythonSimpleDecimalScore() {
        super(TYPE);
    }

    public static PythonSimpleDecimalScore of(int score) {
        var out = new PythonSimpleDecimalScore();
        out.score = new PythonDecimal(BigDecimal.valueOf(score));
        return out;
    }

}
