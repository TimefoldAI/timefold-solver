package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class SimpleScoreInliner extends AbstractScoreInliner<SimpleScore> {

    int score;

    SimpleScoreInliner(boolean constraintMatchEnabled) {
        super(constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<SimpleScore, SimpleScoreContext> buildWeightedScoreImpacter(Constraint constraint,
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
