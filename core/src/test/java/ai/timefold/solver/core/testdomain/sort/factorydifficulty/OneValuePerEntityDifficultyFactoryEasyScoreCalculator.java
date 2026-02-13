package ai.timefold.solver.core.testdomain.sort.factorydifficulty;

import java.util.Objects;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class OneValuePerEntityDifficultyFactoryEasyScoreCalculator
        implements EasyScoreCalculator<TestdataDifficultyFactorySortableSolution, HardSoftScore> {

    @Override
    public @NonNull HardSoftScore
            calculateScore(@NonNull TestdataDifficultyFactorySortableSolution solution) {
        var distinct = (int) solution.getEntityList().stream()
                .map(TestdataDifficultyFactorySortableEntity::getValue)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        var assigned = solution.getEntityList().stream()
                .map(TestdataDifficultyFactorySortableEntity::getValue)
                .filter(Objects::nonNull)
                .count();
        var repeated = (int) (assigned - distinct);
        return HardSoftScore.of(-repeated, -distinct);
    }
}
