package ai.timefold.solver.spring.boot.autoconfigure.dummy.normal.constraints.easy;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.spring.boot.autoconfigure.normal.domain.TestdataSpringSolution;

public class DummySpringEasyScore implements EasyScoreCalculator<TestdataSpringSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataSpringSolution testdataSpringSolution) {
        return null;
    }
}
