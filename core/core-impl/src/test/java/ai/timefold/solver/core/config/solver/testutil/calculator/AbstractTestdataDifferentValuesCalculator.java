package ai.timefold.solver.core.config.solver.testutil.calculator;

import java.util.HashSet;
import java.util.Set;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

public class AbstractTestdataDifferentValuesCalculator implements EasyScoreCalculator<TestdataSolution, SimpleScore> {

    private boolean isCorrupted;
    private int numOfCalls;

    AbstractTestdataDifferentValuesCalculator(boolean isCorrupted) {
        this.isCorrupted = isCorrupted;
    }

    @Override
    public SimpleScore calculateScore(TestdataSolution solution) {
        int score = 0;
        Set<TestdataValue> alreadyUsedValues = new HashSet<>();

        for (TestdataEntity entity : solution.getEntityList()) {
            if (entity.getValue() != null) {
                TestdataValue value = entity.getValue();
                if (alreadyUsedValues.contains(value)) {
                    score -= 1;
                } else {
                    alreadyUsedValues.add(value);
                }
            }
        }
        if (isCorrupted) {
            numOfCalls += 1;
            return SimpleScore.of(score - numOfCalls);
        } else {
            return SimpleScore.of(score);
        }
    }
}
