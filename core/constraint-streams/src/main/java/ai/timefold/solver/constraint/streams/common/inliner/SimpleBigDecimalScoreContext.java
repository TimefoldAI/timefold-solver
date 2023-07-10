package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class SimpleBigDecimalScoreContext extends ScoreContext<SimpleBigDecimalScore, SimpleBigDecimalScoreInliner> {

    public SimpleBigDecimalScoreContext(SimpleBigDecimalScoreInliner parent, Constraint constraint,
            SimpleBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal impact = constraintWeight.score().multiply(matchWeight);
        parent.score = parent.score.add(impact);
        UndoScoreImpacter undoScoreImpact = () -> parent.score = parent.score.subtract(impact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleBigDecimalScore.of(impact), justificationsSupplier);
    }

}
