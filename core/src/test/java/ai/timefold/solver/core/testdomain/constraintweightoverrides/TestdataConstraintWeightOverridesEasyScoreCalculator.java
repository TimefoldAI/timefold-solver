package ai.timefold.solver.core.testdomain.constraintweightoverrides;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataConstraintWeightOverridesEasyScoreCalculator
        implements EasyScoreCalculator<TestdataConstraintWeightOverridesSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataConstraintWeightOverridesSolution solution) {
        var firstWeight = solution.getConstraintWeightOverrides()
                .getConstraintWeight("First weight");
        if (firstWeight != null) {
            return SimpleScore.of(solution.getEntityList().size() * firstWeight.score());
        }
        return SimpleScore.of(solution.getEntityList().size());
    }
}
