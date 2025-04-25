package ai.timefold.solver.core.testdomain.unassignedvar;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;

public class TestdataAllowsUnassignedEasyScoreCalculator
        implements EasyScoreCalculator<TestdataAllowsUnassignedSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataAllowsUnassignedSolution solution) {
        int score = 0;
        for (TestdataAllowsUnassignedEntity left : solution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                score -= 1;
            } else {
                for (TestdataAllowsUnassignedEntity right : solution.getEntityList()) {
                    if (left != right && Objects.equals(right.getValue(), value)) {
                        score -= 1000;
                    }
                }
            }
        }
        return SimpleScore.of(score);
    }
}
