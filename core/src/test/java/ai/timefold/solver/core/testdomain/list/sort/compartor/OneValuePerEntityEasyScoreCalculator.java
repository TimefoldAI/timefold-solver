package ai.timefold.solver.core.testdomain.list.sort.compartor;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListSortableSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataListSortableSolution testdataListSortableSolution) {
        var score = 0;
        for (var entity : testdataListSortableSolution.getEntityList()) {
            if (entity.getValueList().size() == 1) {
                score += 10;
            }
            score++;
        }
        return SimpleScore.of(score);
    }
}
