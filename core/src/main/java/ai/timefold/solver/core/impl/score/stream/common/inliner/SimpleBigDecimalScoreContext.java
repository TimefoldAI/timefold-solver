package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class SimpleBigDecimalScoreContext extends ScoreContext<SimpleBigDecimalScore, SimpleBigDecimalScoreInliner> {

    public SimpleBigDecimalScoreContext(SimpleBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<SimpleBigDecimalScore> constraintMatchSupplier) {
        BigDecimal impact = constraintWeight.score().multiply(matchWeight);
        parent.score = parent.score.add(impact);
        UndoScoreImpacter undoScoreImpact = () -> parent.score = parent.score.subtract(impact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleBigDecimalScore.of(impact), constraintMatchSupplier);
    }

}
