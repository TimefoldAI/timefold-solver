package ai.timefold.solver.core.impl.score;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public class DummySimpleScoreEasyScoreCalculator<Solution_> implements EasyScoreCalculator<Solution_, SimpleScore> {

    @Override
    public SimpleScore calculateScore(Solution_ solution_) {
        return SimpleScore.of(0);
    }

}
