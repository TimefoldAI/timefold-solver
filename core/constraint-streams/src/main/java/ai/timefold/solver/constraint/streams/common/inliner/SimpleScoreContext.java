package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class SimpleScoreContext extends ScoreContext<SimpleScore, SimpleScoreInliner> {

    public SimpleScoreContext(SimpleScoreInliner parent, Constraint constraint, SimpleScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int impact = constraintWeight.score() * matchWeight;
        parent.score += impact;
        UndoScoreImpacter undoScoreImpact = () -> parent.score -= impact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleScore.of(impact), justificationsSupplier);
    }

}
