package ai.timefold.solver.core.testdomain.sort.comparator;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.testdomain.sort.comparator.newapproach.TestdataNewSortableEntity;
import ai.timefold.solver.core.testdomain.sort.comparator.newapproach.TestdataNewSortableSolution;

import org.jspecify.annotations.NonNull;

public class NewOneValuePerEntityEasyScoreCalculator
        implements EasyScoreCalculator<TestdataNewSortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull TestdataNewSortableSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataNewSortableEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataNewSortableEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
