package ai.timefold.solver.python.score;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonSimpleScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("SimpleScore", PythonSimpleScore.class);
    public PythonInteger score;

    public PythonSimpleScore() {
        super(TYPE);
    }

    public static PythonSimpleScore of(int score) {
        var out = new PythonSimpleScore();
        out.score = PythonInteger.valueOf(score);
        return out;
    }

}
