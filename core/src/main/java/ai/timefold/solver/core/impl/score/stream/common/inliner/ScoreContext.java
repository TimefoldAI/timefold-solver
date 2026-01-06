package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public abstract class ScoreContext<Score_ extends Score<Score_>, ScoreInliner_ extends AbstractScoreInliner<Score_>> {

    private final AbstractConstraint<?, ?, ?> constraint;
    protected final Score_ constraintWeight;
    protected final ScoreInliner_ inliner;

    protected ScoreContext(ScoreInliner_ inliner, AbstractConstraint<?, ?, ?> constraint, Score_ constraintWeight) {
        this.constraint = constraint;
        this.constraintWeight = constraintWeight;
        this.inliner = inliner;
    }

    public final AbstractConstraint<?, ?, ?> getConstraint() {
        return constraint;
    }

    public final Score_ getConstraintWeight() {
        return constraintWeight;
    }

    protected final ScoreImpact<Score_> possiblyAddConstraintMatch(ScoreImpact<Score_> scoreImpact,
            ConstraintMatchSupplier<Score_> constraintMatchSupplier) {
        if (!inliner.constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return inliner.addConstraintMatch(constraint, constraintMatchSupplier, scoreImpact);
    }

}
