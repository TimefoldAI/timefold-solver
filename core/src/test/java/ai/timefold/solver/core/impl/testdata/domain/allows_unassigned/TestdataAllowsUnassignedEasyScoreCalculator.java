package ai.timefold.solver.core.impl.testdata.domain.allows_unassigned;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class TestdataAllowsUnassignedEasyScoreCalculator
        implements EasyScoreCalculator<TestdataAllowsUnassignedSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataAllowsUnassignedSolution solution) {
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
