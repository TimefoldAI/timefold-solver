package ai.timefold.solver.core.testdomain.multivar.list.singleentity.unassignedvar;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataUnassignedListMultiVarEasyScoreCalculator
        implements EasyScoreCalculator<TestdataUnassignedListMultiVarSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataUnassignedListMultiVarSolution solution) {
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
            if (entity.getValueList().stream().anyMatch(TestdataUnassignedListMultiVarValue::isBlocked)) {
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
