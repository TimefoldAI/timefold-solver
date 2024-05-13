package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class BendableBigDecimalScoreInliner extends AbstractScoreInliner<BendableBigDecimalScore> {

    final BigDecimal[] hardScores;
    final BigDecimal[] softScores;

    BendableBigDecimalScoreInliner(Map<Constraint, BendableBigDecimalScore> constraintWeightMap, boolean constraintMatchEnabled,
            int hardLevelsSize, int softLevelsSize) {
        super(constraintWeightMap, constraintMatchEnabled);
        hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
    }

    @Override
    public WeightedScoreImpacter<BendableBigDecimalScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        Integer singleLevel = null;
        var constraintWeight = constraintWeightMap.get(constraint);
        for (var i = 0; i < constraintWeight.levelsSize(); i++) {
            if (constraintWeight.hardOrSoftScore(i).signum() != 0) {
                if (singleLevel != null) {
                    singleLevel = null;
                    break;
                }
                singleLevel = i;
            }
        }
        if (singleLevel != null) {
            var isHardScore = singleLevel < constraintWeight.hardLevelsSize();
            var level = isHardScore ? singleLevel : singleLevel - constraintWeight.hardLevelsSize();
            var context = new BendableBigDecimalScoreContext(this, constraint, constraintWeight, hardScores.length,
                    softScores.length, level, constraintWeight.hardOrSoftScore(singleLevel));
            if (isHardScore) {
                return WeightedScoreImpacter.of(context, BendableBigDecimalScoreContext::changeHardScoreBy);
            } else {
                return WeightedScoreImpacter.of(context, BendableBigDecimalScoreContext::changeSoftScoreBy);
            }
        } else {
            var context = new BendableBigDecimalScoreContext(this, constraint, constraintWeight, hardScores.length,
                    softScores.length);
            return WeightedScoreImpacter.of(context, BendableBigDecimalScoreContext::changeScoreBy);
        }
    }

    @Override
    public BendableBigDecimalScore extractScore(int initScore) {
        return BendableBigDecimalScore.ofUninitialized(initScore,
                Arrays.copyOf(hardScores, hardScores.length),
                Arrays.copyOf(softScores, softScores.length));
    }

    @Override
    public String toString() {
        return BendableBigDecimalScore.class.getSimpleName() + " inliner";
    }

}
