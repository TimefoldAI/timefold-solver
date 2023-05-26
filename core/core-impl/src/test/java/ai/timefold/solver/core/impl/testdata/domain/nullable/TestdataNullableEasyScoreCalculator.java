package ai.timefold.solver.core.impl.testdata.domain.nullable;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataNullableEasyScoreCalculator implements EasyScoreCalculator<TestdataNullableSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataNullableSolution solution) {
        int score = 0;
        for (TestdataNullableEntity left : solution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                score -= 1;
            } else {
                for (TestdataNullableEntity right : solution.getEntityList()) {
                    if (left != right && Objects.equals(right.getValue(), value)) {
                        score -= 1000;
                    }
                }
            }
        }
        return SimpleScore.of(score);
    }
}
