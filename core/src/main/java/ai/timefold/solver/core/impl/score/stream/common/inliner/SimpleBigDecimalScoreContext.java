package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class SimpleBigDecimalScoreContext extends ScoreContext<SimpleBigDecimalScore, SimpleBigDecimalScoreInliner> {

    public SimpleBigDecimalScoreContext(SimpleBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<SimpleBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<SimpleBigDecimalScore> constraintMatchSupplier) {
        var impact = constraintWeight.score().multiply(matchWeight);
        parent.score = parent.score.add(impact);
        var scoreImpact = new SimpleBigDecimalImpact(parent, impact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SimpleBigDecimalImpact(SimpleBigDecimalScoreInliner inliner, BigDecimal impact)
            implements
                ScoreImpact<SimpleBigDecimalScore> {

        @Override
        public AbstractScoreInliner<SimpleBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.score = inliner.score.subtract(impact);
        }

        @Override
        public SimpleBigDecimalScore toScore() {
            return SimpleBigDecimalScore.of(impact);
        }

    }

}
