package ai.timefold.solver.python.score;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonSimpleScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("SimpleScore", PythonSimpleScore.class);
    public PythonInteger init_score;
    public PythonInteger score;

    public PythonSimpleScore() {
        super(TYPE);
    }

    public static PythonSimpleScore of(int score) {
        var out = new PythonSimpleScore();
        out.init_score = PythonInteger.ZERO;
        out.score = PythonInteger.valueOf(score);
        return out;
    }

    public static PythonSimpleScore ofUninitialized(int initScore, int score) {
        var out = new PythonSimpleScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.score = PythonInteger.valueOf(score);
        return out;
    }
}
