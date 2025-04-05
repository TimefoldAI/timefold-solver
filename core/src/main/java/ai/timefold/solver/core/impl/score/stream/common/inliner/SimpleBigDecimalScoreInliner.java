package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class SimpleBigDecimalScoreInliner extends AbstractScoreInliner<SimpleBigDecimalScore> {

    BigDecimal score = BigDecimal.ZERO;

    SimpleBigDecimalScoreInliner(Map<Constraint, SimpleBigDecimalScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<SimpleBigDecimalScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        SimpleBigDecimalScore constraintWeight = constraintWeightMap.get(constraint);
        SimpleBigDecimalScoreContext context = new SimpleBigDecimalScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, SimpleBigDecimalScoreContext::changeScoreBy);
    }

    @Override
    public SimpleBigDecimalScore extractScore() {
        return SimpleBigDecimalScore.of(score);
    }

    @Override
    public String toString() {
        return SimpleBigDecimalScore.class.getSimpleName() + " inliner";
    }

}
