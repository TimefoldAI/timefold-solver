package ai.timefold.solver.quarkus.testdomain.dummy;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.quarkus.testdomain.normal.TestdataQuarkusSolution;

import org.jspecify.annotations.NonNull;

public class DummyTestdataQuarkusEasyScoreCalculator implements EasyScoreCalculator<TestdataQuarkusSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataQuarkusSolution testdataQuarkusSolution) {
        return null;
    }
}
