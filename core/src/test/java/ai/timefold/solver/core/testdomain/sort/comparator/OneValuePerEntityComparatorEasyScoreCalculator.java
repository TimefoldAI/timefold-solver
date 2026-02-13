package ai.timefold.solver.core.testdomain.sort.comparator;

import java.util.Objects;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityComparatorEasyScoreCalculator
        implements EasyScoreCalculator<TestdataComparatorSortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull TestdataComparatorSortableSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataComparatorSortableEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataComparatorSortableEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
