package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;

final class SimpleBigDecimalScoreInliner extends AbstractScoreInliner<SimpleBigDecimalScore> {

    BigDecimal score = BigDecimal.ZERO;

    SimpleBigDecimalScoreInliner(boolean constraintMatchEnabled) {
        this(1, constraintMatchEnabled);
    }

    SimpleBigDecimalScoreInliner(int constraintCount, boolean constraintMatchEnabled) {
        super(constraintCount, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<SimpleBigDecimalScoreContext> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint, SimpleBigDecimalScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        SimpleBigDecimalScoreContext context = new SimpleBigDecimalScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, SimpleBigDecimalScoreContext::changeScoreBy);
    }

    @Override
    public SimpleBigDecimalScore extractScore(int initScore) {
        return SimpleBigDecimalScore.ofUninitialized(initScore, score);
    }

    @Override
    public String toString() {
        return SimpleBigDecimalScore.class.getSimpleName() + " inliner";
    }

}
