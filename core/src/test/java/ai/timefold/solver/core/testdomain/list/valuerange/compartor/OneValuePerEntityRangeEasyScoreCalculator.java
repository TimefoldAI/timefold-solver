package ai.timefold.solver.core.testdomain.list.valuerange.compartor;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityRangeEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListSortableEntityProvidingSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore
            calculateScore(@NonNull TestdataListSortableEntityProvidingSolution testdataListSortableEntityProvidingSolution) {
        var score = 0;
        for (var entity : testdataListSortableEntityProvidingSolution.getEntityList()) {
            if (entity.getValueList().size() == 1) {
                score += 10;
            }
            score++;
        }
        return SimpleScore.of(score);
    }
}
