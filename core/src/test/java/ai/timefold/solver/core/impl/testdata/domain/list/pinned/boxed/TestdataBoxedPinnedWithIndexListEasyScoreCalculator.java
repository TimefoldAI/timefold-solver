package ai.timefold.solver.core.impl.testdata.domain.list.pinned.boxed;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public final class TestdataBoxedPinnedWithIndexListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataBoxedPinnedWithIndexListSolution, SimpleScore> {

    @Override
    public @NonNull SimpleScore calculateScore(@NonNull TestdataBoxedPinnedWithIndexListSolution solution) {
        return SimpleScore.of(-solution.getEntityList().size());
    }
}
