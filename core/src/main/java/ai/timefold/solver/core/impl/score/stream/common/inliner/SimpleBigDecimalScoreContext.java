package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class SimpleBigDecimalScoreContext extends ScoreContext<SimpleBigDecimalScore, SimpleBigDecimalScoreInliner> {

    public SimpleBigDecimalScoreContext(SimpleBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<SimpleBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            @Nullable ConstraintMatchSupplier<SimpleBigDecimalScore> constraintMatchSupplier) {
        var impact = constraintWeight.score().multiply(matchWeight);
        inliner.score = inliner.score.add(impact);
        var scoreImpact = new Impact(inliner, impact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record Impact(SimpleBigDecimalScoreInliner inliner, BigDecimal impact)
            implements
                ScoreImpact<SimpleBigDecimalScore> {

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
