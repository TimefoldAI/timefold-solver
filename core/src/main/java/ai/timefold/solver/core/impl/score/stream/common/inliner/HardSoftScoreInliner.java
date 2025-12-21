package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftScoreInliner extends AbstractScoreInliner<HardSoftScore> {

    int hardScore;
    int softScore;

    HardSoftScoreInliner(Map<Constraint, HardSoftScore> constraintWeightMap, ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<HardSoftScore, ?> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint) {
        var constraintWeight = constraintWeightMap.get(constraint);
        var context = new HardSoftScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeScoreBy);
    }

    @Override
    public HardSoftScore extractScore() {
        return HardSoftScore.of(hardScore, softScore);
    }

    @Override
    public String toString() {
        return HardSoftScore.class.getSimpleName() + " inliner";
    }

}
