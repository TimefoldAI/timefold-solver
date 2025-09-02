package ai.timefold.solver.quarkus.testdomain.declarative.missing;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataQuarkusDeclarativeMissingSupplierEasyScoreCalculator
        implements EasyScoreCalculator<TestdataQuarkusDeclarativeMissingSupplierSolution, HardSoftScore> {
    @Override
    public @NonNull HardSoftScore calculateScore(
            @NonNull TestdataQuarkusDeclarativeMissingSupplierSolution testdataQuarkusDeclarativeMissingSupplierSolution) {
        return HardSoftScore.ZERO;
    }
}
