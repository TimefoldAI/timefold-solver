package ai.timefold.solver.core.testdomain.mixed.singleentity.unassignedvar;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataUnassignedMixedEasyScoreCalculator
        implements EasyScoreCalculator<TestdataUnassignedMixedSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataUnassignedMixedSolution solution) {
        int score = 0;
        for (var entity : solution.getEntityList()) {
            if (entity.getBasicValue() != null && !entity.getBasicValue().isBlocked()) {
                score++;
            } else if (entity.getBasicValue() != null) {
                score -= 10;
            }
            if (entity.getSecondBasicValue() != null) {
                score++;
            }
            if (entity.getValueList().stream().anyMatch(TestdataUnassignedMixedValue::isBlocked)) {
                score -= 10;
            } else if (entity.getValueList().size() == 3) {
                score += 2;
            } else {
                score++;
            }
        }
        return SimpleScore.of(score);
    }

}
