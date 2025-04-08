package ai.timefold.solver.python.score;

import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonHardSoftScore extends AbstractPythonLikeObject {
    public static final PythonLikeType TYPE = new PythonLikeType("HardSoftScore", PythonHardSoftScore.class);
    public PythonInteger hard_score;
    public PythonInteger soft_score;

    public PythonHardSoftScore() {
        super(TYPE);
    }

    public static PythonHardSoftScore of(int hardScore, int softScore) {
        var out = new PythonHardSoftScore();
        out.hard_score = PythonInteger.valueOf(hardScore);
        out.soft_score = PythonInteger.valueOf(softScore);
        return out;
    }

}
