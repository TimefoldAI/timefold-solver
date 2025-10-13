package ai.timefold.solver.core.testdomain.sort.factory.oldapproach;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OldOneValuePerEntityFactoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataFactoryOldSortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataFactoryOldSortableSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataFactoryOldSortableEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataFactoryOldSortableEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
