package ai.timefold.solver.python.score;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonHardMediumSoftScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("HardMediumSoftScore", PythonHardMediumSoftScore.class);
    public PythonInteger init_score;
    public PythonInteger hard_score;
    public PythonInteger medium_score;
    public PythonInteger soft_score;

    public PythonHardMediumSoftScore() {
        super(TYPE);
    }

    public static PythonHardMediumSoftScore of(int hardScore, int mediumScore, int softScore) {
        var out = new PythonHardMediumSoftScore();
        out.init_score = PythonInteger.ZERO;
        out.hard_score = PythonInteger.valueOf(hardScore);
        out.medium_score = PythonInteger.valueOf(mediumScore);
        out.soft_score = PythonInteger.valueOf(softScore);
        return out;
    }

    public static PythonHardMediumSoftScore ofUninitialized(int initScore, int hardScore, int mediumScore, int softScore) {
        var out = new PythonHardMediumSoftScore();
        out.init_score = PythonInteger.valueOf(initScore);
        out.hard_score = PythonInteger.valueOf(hardScore);
        out.medium_score = PythonInteger.valueOf(mediumScore);
        out.soft_score = PythonInteger.valueOf(softScore);
        return out;
    }
}
