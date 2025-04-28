package ai.timefold.solver.core.testdomain.cascade.single;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestdataSingleCascadingEasyScoreCalculator implements
        EasyScoreCalculator<TestdataSingleCascadingSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(TestdataSingleCascadingSolution solution) {
        return SimpleScore.of((int) solution.getValueList().stream().filter(e -> e.getCascadeValue() != null).count());
    }
}
