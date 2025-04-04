package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class SimpleScoreInliner extends AbstractScoreInliner<SimpleScore> {

    int score;

    SimpleScoreInliner(Map<Constraint, SimpleScore> constraintWeightMap, ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<SimpleScore, ?> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint) {
        SimpleScore constraintWeight = constraintWeightMap.get(constraint);
        SimpleScoreContext context = new SimpleScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, SimpleScoreContext::changeScoreBy);
    }

    @Override
    public SimpleScore extractScore() {
        return SimpleScore.of(score);
    }

    @Override
    public String toString() {
        return SimpleScore.class.getSimpleName() + " inliner";
    }

}
