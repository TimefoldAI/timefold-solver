package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public abstract class ScoreContext<Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> {

    protected final ScoreInliner_ inliner;
    protected final AbstractConstraint<?, ?, ?> constraint;
    protected final Score_ constraintWeight;
    protected final ConstraintMatchPolicy constraintMatchPolicy;

    protected ScoreContext(ScoreInliner_ inliner, AbstractConstraint<?, ?, ?> constraint, Score_ constraintWeight) {
        this.inliner = inliner;
        this.constraint = constraint;
        this.constraintWeight = constraintWeight;
        this.constraintMatchPolicy = inliner.constraintMatchPolicy;
    }

    public AbstractConstraint<?, ?, ?> getConstraint() {
        return constraint;
    }

    public Score_ getConstraintWeight() {
        return constraintWeight;
    }

    protected ScoreImpact<Score_> impactWithConstraintMatch(ScoreImpact<Score_> scoreImpact,
            ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        return inliner.addConstraintMatch(constraint, constraintMatchSupplier, scoreImpact);
    }

}
