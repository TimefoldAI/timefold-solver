package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;
import java.util.Arrays;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;

final class BendableBigDecimalScoreInliner extends AbstractScoreInliner<BendableBigDecimalScore> {

    final BigDecimal[] hardScores;
    final BigDecimal[] softScores;

    BendableBigDecimalScoreInliner(boolean constraintMatchEnabled, int hardLevelsSize, int softLevelsSize) {
        this(1, constraintMatchEnabled, hardLevelsSize, softLevelsSize);
    }

    BendableBigDecimalScoreInliner(int constraintCount, boolean constraintMatchEnabled, int hardLevelsSize,
            int softLevelsSize) {
        super(constraintCount, constraintMatchEnabled);
        hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
    }

    @Override
    public WeightedScoreImpacter<BendableBigDecimalScoreContext> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint, BendableBigDecimalScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        Integer singleLevel = null;
        for (int i = 0; i < constraintWeight.levelsSize(); i++) {
            if (!constraintWeight.hardOrSoftScore(i).equals(BigDecimal.ZERO)) {
                if (singleLevel != null) {
                    singleLevel = null;
                    break;
                }
                singleLevel = i;
            }
        }
        if (singleLevel != null) {
            boolean isHardScore = singleLevel < constraintWeight.hardLevelsSize();
            int level = isHardScore ? singleLevel : singleLevel - constraintWeight.hardLevelsSize();
            BendableBigDecimalScoreContext context = new BendableBigDecimalScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length, level, constraintWeight.hardOrSoftScore(singleLevel));
            if (isHardScore) {
                return WeightedScoreImpacter.of(context, BendableBigDecimalScoreContext::changeHardScoreBy);
            } else {
                return WeightedScoreImpacter.of(context, BendableBigDecimalScoreContext::changeSoftScoreBy);
            }
        } else {
            BendableBigDecimalScoreContext context = new BendableBigDecimalScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length);
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
