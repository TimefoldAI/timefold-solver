package ai.timefold.solver.quarkus.testdomain.dummy;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.quarkus.testdomain.shadowvariable.domain.TestdataQuarkusShadowVariableSolution;

import org.jspecify.annotations.NonNull;

public class DummyTestdataQuarkusShadowVariableEasyScoreCalculator
        implements EasyScoreCalculator<TestdataQuarkusShadowVariableSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataQuarkusShadowVariableSolution testdataQuarkusSolution) {
        return null;
    }
}
