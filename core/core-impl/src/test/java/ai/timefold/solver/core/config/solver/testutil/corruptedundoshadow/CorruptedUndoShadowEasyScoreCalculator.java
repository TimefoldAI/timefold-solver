package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

public class CorruptedUndoShadowEasyScoreCalculator implements EasyScoreCalculator<CorruptedUndoShadowSolution, SimpleScore> {
    @Override
    public SimpleScore calculateScore(CorruptedUndoShadowSolution corruptedUndoShadowSolution) {
        int score = 0;
        for (CorruptedUndoShadowEntity entity : corruptedUndoShadowSolution.entityList) {
            if (Objects.equals(entity.value, entity.valueClone)) {
                score++;
            }
        }
        return SimpleScore.of(score);
    }
}
