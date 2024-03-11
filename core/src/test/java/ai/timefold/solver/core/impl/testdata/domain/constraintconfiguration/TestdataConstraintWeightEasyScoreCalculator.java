package ai.timefold.solver.core.impl.testdata.domain.constraintconfiguration;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public final class TestdataConstraintWeightEasyScoreCalculator
        implements EasyScoreCalculator<TestdataConstraintConfigurationSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataConstraintConfigurationSolution solution) {
        SimpleScore constraintWeight = solution.getConstraintConfiguration().getFirstWeight();
        return constraintWeight.multiply(solution.getEntityList().size());
    }
}
