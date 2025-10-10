package ai.timefold.solver.core.testdomain.list.sort.factory;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityFactoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListFactorySortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataListFactorySortableSolution testdataListFactorySortableSolution) {
        var softScore = 0;
        var hardScore = 0;
        for (var entity : testdataListFactorySortableSolution.getEntityList()) {
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
