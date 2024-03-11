package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

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
