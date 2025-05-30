package ai.timefold.solver.core.testdomain.mixed.singleentity;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataMixedEasyScoreCalculator implements EasyScoreCalculator<TestdataMixedSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataMixedSolution solution) {
        int score = 0;
        for (var entity : solution.getEntityList()) {
            if (entity.getBasicValue() != null) {
                score++;
            }
            if (entity.getSecondBasicValue() != null) {
                score++;
            }
            if (entity.getValueList().size() == 1) {
                score += 2;
            } else {
                score += 1;
            }
        }
        return SimpleScore.of(score);
    }

}
