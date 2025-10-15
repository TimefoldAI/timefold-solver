package ai.timefold.solver.core.testdomain.valuerange.sort.comparator.newapproach;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class NewOneValuePerEntityRangeEasyScoreCalculator
        implements EasyScoreCalculator<TestdataNewSortableEntityProvidingSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataNewSortableEntityProvidingSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataNewSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataNewSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
