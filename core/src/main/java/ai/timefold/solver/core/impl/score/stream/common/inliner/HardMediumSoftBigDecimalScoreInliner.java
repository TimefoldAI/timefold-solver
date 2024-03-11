package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftBigDecimalScoreInliner extends AbstractScoreInliner<HardMediumSoftBigDecimalScore> {

    BigDecimal hardScore = BigDecimal.ZERO;
    BigDecimal mediumScore = BigDecimal.ZERO;
    BigDecimal softScore = BigDecimal.ZERO;

    HardMediumSoftBigDecimalScoreInliner(Map<Constraint, HardMediumSoftBigDecimalScore> constraintWeightMap,
            boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftBigDecimalScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        HardMediumSoftBigDecimalScore constraintWeight = constraintWeightMap.get(constraint);
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
