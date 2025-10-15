package ai.timefold.solver.core.testdomain.valuerange.sort.factory.oldapproach;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OldOneValuePerEntityRangeFactoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataFactoryOldSortableEntityProvidingSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(
                    @NonNull TestdataFactoryOldSortableEntityProvidingSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataFactoryOldSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataFactoryOldSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
