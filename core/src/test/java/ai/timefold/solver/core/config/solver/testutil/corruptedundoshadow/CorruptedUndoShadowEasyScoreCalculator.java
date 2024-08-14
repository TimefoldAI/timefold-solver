package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;

import org.jspecify.annotations.NonNull;

public class CorruptedUndoShadowEasyScoreCalculator implements EasyScoreCalculator<CorruptedUndoShadowSolution, SimpleScore> {
    @Override
    public @NonNull SimpleScore calculateScore(@NonNull CorruptedUndoShadowSolution corruptedUndoShadowSolution) {
        int score = 0;
        for (CorruptedUndoShadowEntity entity : corruptedUndoShadowSolution.entityList) {
            if (Objects.equals(entity.value, entity.valueClone)) {
                score++;
            }
        }
        return SimpleScore.of(score);
    }
}
