package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;

final class HardMediumSoftBigDecimalScoreInliner extends AbstractScoreInliner<HardMediumSoftBigDecimalScore> {

    BigDecimal hardScore = BigDecimal.ZERO;
    BigDecimal mediumScore = BigDecimal.ZERO;
    BigDecimal softScore = BigDecimal.ZERO;

    HardMediumSoftBigDecimalScoreInliner(boolean constraintMatchEnabled) {
        this(1, constraintMatchEnabled);
    }

    HardMediumSoftBigDecimalScoreInliner(int constraintCount, boolean constraintMatchEnabled) {
        super(constraintCount, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftBigDecimalScoreContext>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint,
                    HardMediumSoftBigDecimalScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        BigDecimal hardConstraintWeight = constraintWeight.hardScore();
        BigDecimal mediumConstraintWeight = constraintWeight.mediumScore();
        BigDecimal softConstraintWeight = constraintWeight.softScore();
        HardMediumSoftBigDecimalScoreContext context =
                new HardMediumSoftBigDecimalScoreContext(this, constraint, constraintWeight);
        if (mediumConstraintWeight.equals(BigDecimal.ZERO) && softConstraintWeight.equals(BigDecimal.ZERO)) {
            return WeightedScoreImpacter.of(context, HardMediumSoftBigDecimalScoreContext::changeHardScoreBy);
        } else if (hardConstraintWeight.equals(BigDecimal.ZERO) && softConstraintWeight.equals(BigDecimal.ZERO)) {
            return WeightedScoreImpacter.of(context, HardMediumSoftBigDecimalScoreContext::changeMediumScoreBy);
        } else if (hardConstraintWeight.equals(BigDecimal.ZERO) && mediumConstraintWeight.equals(BigDecimal.ZERO)) {
            return WeightedScoreImpacter.of(context, HardMediumSoftBigDecimalScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context, HardMediumSoftBigDecimalScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardMediumSoftBigDecimalScore extractScore(int initScore) {
        return HardMediumSoftBigDecimalScore.ofUninitialized(initScore, hardScore, mediumScore, softScore);
    }

    @Override
    public String toString() {
        return HardMediumSoftBigDecimalScore.class.getSimpleName() + " inliner";
    }

}
