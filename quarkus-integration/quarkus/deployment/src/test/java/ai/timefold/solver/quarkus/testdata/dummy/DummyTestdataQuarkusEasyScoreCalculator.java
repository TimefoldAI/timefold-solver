package ai.timefold.solver.quarkus.testdata.dummy;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.quarkus.testdata.normal.domain.TestdataQuarkusSolution;

import org.jspecify.annotations.NonNull;

public class DummyTestdataQuarkusEasyScoreCalculator implements EasyScoreCalculator<TestdataQuarkusSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataQuarkusSolution testdataQuarkusSolution) {
        return null;
    }
}
