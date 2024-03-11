package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class SimpleScoreContext extends ScoreContext<SimpleScore, SimpleScoreInliner> {

    public SimpleScoreContext(SimpleScoreInliner parent, AbstractConstraint<?, ?, ?> constraint, SimpleScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(int matchWeight, ConstraintMatchSupplier<SimpleScore> constraintMatchSupplier) {
        int impact = constraintWeight.score() * matchWeight;
        parent.score += impact;
        UndoScoreImpacter undoScoreImpact = () -> parent.score -= impact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleScore.of(impact), constraintMatchSupplier);
    }

}
