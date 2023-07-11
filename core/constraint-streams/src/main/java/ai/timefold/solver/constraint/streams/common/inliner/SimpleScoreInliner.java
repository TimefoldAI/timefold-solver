package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

final class SimpleScoreInliner extends AbstractScoreInliner<SimpleScore> {

    int score;

    SimpleScoreInliner(boolean constraintMatchEnabled) {
        this(1, constraintMatchEnabled);
    }

    SimpleScoreInliner(int constraintCount, boolean constraintMatchEnabled) {
        super(constraintCount, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<SimpleScoreContext> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint,
            SimpleScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        SimpleScoreContext context = new SimpleScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, SimpleScoreContext::changeScoreBy);
    }

    @Override
    public SimpleScore extractScore(int initScore) {
        return SimpleScore.ofUninitialized(initScore, score);
    }

    @Override
    public String toString() {
        return SimpleScore.class.getSimpleName() + " inliner";
    }

}
