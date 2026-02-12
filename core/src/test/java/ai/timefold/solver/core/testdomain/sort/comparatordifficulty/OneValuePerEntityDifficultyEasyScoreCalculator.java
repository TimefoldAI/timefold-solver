package ai.timefold.solver.core.testdomain.sort.comparatordifficulty;

import java.util.Objects;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityDifficultyEasyScoreCalculator
        implements EasyScoreCalculator<TestdataDifficultySortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore calculateScore(@NonNull TestdataDifficultySortableSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataDifficultySortableEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataDifficultySortableEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
