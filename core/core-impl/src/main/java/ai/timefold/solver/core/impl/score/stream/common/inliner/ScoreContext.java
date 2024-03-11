package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public abstract class ScoreContext<Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> {

    protected final ScoreInliner_ parent;
    protected final AbstractConstraint<?, ?, ?> constraint;
    protected final Score_ constraintWeight;
    protected final boolean constraintMatchEnabled;

    protected ScoreContext(ScoreInliner_ parent, AbstractConstraint<?, ?, ?> constraint, Score_ constraintWeight) {
        this.parent = parent;
        this.constraint = constraint;
        this.constraintWeight = constraintWeight;
        this.constraintMatchEnabled = parent.constraintMatchEnabled;
    }

    public AbstractConstraint<?, ?, ?> getConstraint() {
        return constraint;
    }

    public Score_ getConstraintWeight() {
        return constraintWeight;
    }

    public boolean isConstraintMatchEnabled() {
        return constraintMatchEnabled;
    }

    protected UndoScoreImpacter impactWithConstraintMatch(UndoScoreImpacter undoScoreImpact, Score_ score,
            ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        return parent.addConstraintMatch(constraint, score, constraintMatchSupplier, undoScoreImpact);
    }

}
