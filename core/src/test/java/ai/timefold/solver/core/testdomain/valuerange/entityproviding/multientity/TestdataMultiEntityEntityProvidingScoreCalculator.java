package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multientity;

import java.util.Objects;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

/**
 * Pairs the entities of both entity lists by index,
 * and rewards each pair whose {@code value} matches its {@code solutionValue}.
 */
public class TestdataMultiEntityEntityProvidingScoreCalculator
        implements EasyScoreCalculator<TestdataMultiEntityEntityProvidingSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataMultiEntityEntityProvidingSolution solution) {
        var entityProvidingEntityList = solution.getEntityProvidingEntityList();
        var solutionRangeEntityList = solution.getSolutionRangeEntityList();
        var score = 0;
        for (var i = 0; i < entityProvidingEntityList.size(); i++) {
            var value = entityProvidingEntityList.get(i).getValue();
            if (value != null && Objects.equals(value, solutionRangeEntityList.get(i).getSolutionValue())) {
                score++;
            }
        }
        return SimpleScore.of(score);
    }

}
