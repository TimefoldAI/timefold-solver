package ai.timefold.solver.core.testdomain.shadow.inverserelation;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class TestdataInverseRelationEasyScoreCalculator
        implements EasyScoreCalculator<TestdataInverseRelationSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataInverseRelationSolution testdataInverseRelationSolution) {
        int score = 0;
        for (var value : testdataInverseRelationSolution.getValueList()) {
            score -= value.getEntities().size() * value.getEntities().size();
        }
        return SimpleScore.of(score);
    }
}
