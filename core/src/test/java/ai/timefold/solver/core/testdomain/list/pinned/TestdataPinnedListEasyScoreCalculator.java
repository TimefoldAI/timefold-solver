package ai.timefold.solver.core.testdomain.list.pinned;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataPinnedListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataPinnedListSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataPinnedListSolution solution) {
        return SimpleScore.of(-solution.getEntityList().size());
    }
}
