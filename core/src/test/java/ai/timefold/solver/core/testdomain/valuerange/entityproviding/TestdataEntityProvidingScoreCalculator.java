package ai.timefold.solver.core.testdomain.valuerange.entityproviding;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataEntityProvidingScoreCalculator
        implements EasyScoreCalculator<TestdataEntityProvidingSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataEntityProvidingSolution solution) {
        int score = 0;
        for (var entity : solution.getEntityList()) {
            if (entity.getValue() == null) {
                score -= 1;
            } else {
                score += 1;
            }
        }
        return SimpleScore.of(score);
    }

}
