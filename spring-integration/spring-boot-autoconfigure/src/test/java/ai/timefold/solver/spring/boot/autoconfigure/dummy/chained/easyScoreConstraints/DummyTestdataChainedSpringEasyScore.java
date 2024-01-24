package ai.timefold.solver.spring.boot.autoconfigure.dummy.chained.easyScoreConstraints;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.spring.boot.autoconfigure.chained.domain.TestdataChainedSpringSolution;

public class DummyTestdataChainedSpringEasyScore implements EasyScoreCalculator<TestdataChainedSpringSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataChainedSpringSolution testdataSpringSolution) {
        return null;
    }
}
