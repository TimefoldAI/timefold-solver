package ai.timefold.solver.core.testdomain.list.valuerange.sort.comparator;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class ListOneValuePerEntityRangeEasyScoreCalculator
        implements EasyScoreCalculator<TestdataListSortableEntityProvidingSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataListSortableEntityProvidingSolution solution) {
        var softScore = 0;
        var hardScore = 0;
        for (var entity : solution.getEntityList()) {
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
