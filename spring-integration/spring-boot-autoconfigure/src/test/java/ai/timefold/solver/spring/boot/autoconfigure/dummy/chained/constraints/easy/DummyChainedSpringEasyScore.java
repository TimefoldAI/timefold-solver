package ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.constraints.easy;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;

public class DummyChainedSpringEasyScore implements EasyScoreCalculator<TestdataChainedSpringSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataChainedSpringSolution testdataSpringSolution) {
        return null;
    }
}
