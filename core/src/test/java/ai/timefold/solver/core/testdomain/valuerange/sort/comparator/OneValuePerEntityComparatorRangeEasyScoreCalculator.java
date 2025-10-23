package ai.timefold.solver.core.testdomain.valuerange.sort.comparator;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityComparatorRangeEasyScoreCalculator
        implements EasyScoreCalculator<TestdataComparatorSortableEntityProvidingSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataComparatorSortableEntityProvidingSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataComparatorSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataComparatorSortableEntityProvidingEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
