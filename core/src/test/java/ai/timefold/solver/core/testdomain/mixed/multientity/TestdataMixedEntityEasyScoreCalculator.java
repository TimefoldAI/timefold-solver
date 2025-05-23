package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataMixedEntityEasyScoreCalculator
        implements EasyScoreCalculator<TestdataMixedMultiEntitySolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataMixedMultiEntitySolution solution) {
        int score = 0;
        for (var entity : solution.getEntityList()) {
            if (entity.getValueList().size() == 1) {
                score += 2;
            } else {
                score += 1;
            }
        }
        for (var entity : solution.getOtherEntityList()) {
            if (entity.getBasicValue() != null) {
                score++;
            }
            if (entity.getSecondBasicValue() != null) {
                score++;
            }
        }
        return SimpleScore.of(score);
    }

}
