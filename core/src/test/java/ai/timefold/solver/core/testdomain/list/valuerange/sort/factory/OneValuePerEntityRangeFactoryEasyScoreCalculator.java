package ai.timefold.solver.core.testdomain.list.valuerange.sort.factory;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityRangeFactoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListFactorySortableEntityProvidingSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(
                    @NonNull TestdataListFactorySortableEntityProvidingSolution testdataListFactorySortableEntityProvidingSolution) {
        var softScore = 0;
        var hardScore = 0;
        for (var entity : testdataListFactorySortableEntityProvidingSolution.getEntityList()) {
            if (entity.getValueList().size() == 1) {
                softScore -= 10;
            } else {
                hardScore -= 10;
            }
            hardScore--;
        }
        return HardSoftScore.of(hardScore, softScore);
    }
}
