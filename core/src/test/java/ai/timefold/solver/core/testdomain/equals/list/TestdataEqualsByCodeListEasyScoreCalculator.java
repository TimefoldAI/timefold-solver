package ai.timefold.solver.core.testdomain.equals.list;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataEqualsByCodeListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataEqualsByCodeListSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataEqualsByCodeListSolution solution) {
        return SimpleScore.of(-solution.getEntityList().size());
    }
}
