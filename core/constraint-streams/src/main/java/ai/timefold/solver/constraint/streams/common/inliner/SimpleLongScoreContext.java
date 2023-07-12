package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;

final class SimpleLongScoreContext extends ScoreContext<SimpleLongScore, SimpleLongScoreInliner> {

    public SimpleLongScoreContext(SimpleLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight, ConstraintMatchSupplier<SimpleLongScore> constraintMatchSupplier) {
        long impact = constraintWeight.score() * matchWeight;
        parent.score += impact;
        UndoScoreImpacter undoScoreImpact = () -> parent.score -= impact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleLongScore.of(impact), constraintMatchSupplier);
    }

}
