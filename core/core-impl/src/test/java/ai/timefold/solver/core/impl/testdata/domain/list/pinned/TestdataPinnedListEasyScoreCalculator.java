package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public final class TestdataPinnedListEasyScoreCalculator
        implements EasyScoreCalculator<TestdataPinnedListSolution, SimpleScore> {

    @Override
    public SimpleScore calculateScore(TestdataPinnedListSolution solution) {
        return SimpleScore.of(-solution.getEntityList().size());
    }
}
