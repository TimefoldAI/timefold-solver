package ai.timefold.solver.core.impl.testdata.domain.list.pinned.index;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public final class TestdataPinnedWithIndexListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataPinnedWithIndexListSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataPinnedWithIndexListSolution solution) {
        return SimpleScore.of(-solution.getEntityList().size());
    }
}
