package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class SimpleLongScoreContext extends ScoreContext<SimpleLongScore, SimpleLongScoreInliner> {

    public SimpleLongScoreContext(SimpleLongScoreInliner parent, Constraint constraint,
            SimpleLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight, JustificationsSupplier justificationsSupplier) {
        long impact = constraintWeight.score() * matchWeight;
        parent.score += impact;
        UndoScoreImpacter undoScoreImpact = () -> parent.score -= impact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, SimpleLongScore.of(impact), justificationsSupplier);
    }

}
