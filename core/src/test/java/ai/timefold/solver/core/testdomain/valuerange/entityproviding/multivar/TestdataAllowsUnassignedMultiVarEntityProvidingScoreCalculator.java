package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multivar;

import java.util.Objects;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

/**
 * Rewards each entity whose entity-range variable {@code value} matches its solution-range variable {@code solutionValue}.
 */
public class TestdataAllowsUnassignedMultiVarEntityProvidingScoreCalculator
        implements EasyScoreCalculator<TestdataAllowsUnassignedMultiVarEntityProvidingSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore
            calculateScore(@NonNull TestdataAllowsUnassignedMultiVarEntityProvidingSolution solution) {
        var score = 0;
        for (var entity : solution.getEntityList()) {
            if (entity.getValue() != null && Objects.equals(entity.getValue(), entity.getSolutionValue())) {
                score++;
            }
        }
        return SimpleScore.of(score);
    }

}
