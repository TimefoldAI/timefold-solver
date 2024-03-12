package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftBigDecimalScoreInliner extends AbstractScoreInliner<HardSoftBigDecimalScore> {

    BigDecimal hardScore = BigDecimal.ZERO;
    BigDecimal softScore = BigDecimal.ZERO;

    HardSoftBigDecimalScoreInliner(Map<Constraint, HardSoftBigDecimalScore> constraintWeightMap,
            boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardSoftBigDecimalScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        HardSoftBigDecimalScore constraintWeight = constraintWeightMap.get(constraint);
        HardSoftBigDecimalScoreContext context = new HardSoftBigDecimalScoreContext(this, constraint, constraintWeight);
        if (constraintWeight.softScore().equals(BigDecimal.ZERO)) {
            return WeightedScoreImpacter.of(context, HardSoftBigDecimalScoreContext::changeHardScoreBy);
        } else if (constraintWeight.hardScore().equals(BigDecimal.ZERO)) {
            return WeightedScoreImpacter.of(context, HardSoftBigDecimalScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context, HardSoftBigDecimalScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardSoftBigDecimalScore extractScore(int initScore) {
        return HardSoftBigDecimalScore.ofUninitialized(initScore, hardScore, softScore);
    }

    @Override
    public String toString() {
        return HardSoftBigDecimalScore.class.getSimpleName() + " inliner";
    }

}
