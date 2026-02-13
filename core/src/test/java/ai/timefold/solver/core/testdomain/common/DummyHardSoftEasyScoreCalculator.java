package ai.timefold.solver.core.testdomain.common;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class DummyHardSoftEasyScoreCalculator implements EasyScoreCalculator<Object, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull Object o) {
        return HardSoftScore.ZERO;
    }
}
