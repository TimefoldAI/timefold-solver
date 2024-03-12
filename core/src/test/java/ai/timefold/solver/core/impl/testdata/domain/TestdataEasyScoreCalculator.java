package ai.timefold.solver.core.impl.testdata.domain;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public class TestdataEasyScoreCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataSolution solution) {
        int score = 0;
        for (TestdataEntity left : solution.getEntityList()) {
            TestdataValue value = left.getValue();
            if (value == null) {
                continue;
            }
            for (TestdataEntity right : solution.getEntityList()) {
                if (left != right && Objects.equals(right.getValue(), value)) {
                    score -= 1;
                }
            }
        }
        return SimpleScore.of(score);
    }
}
