package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftScoreInliner extends AbstractScoreInliner<HardMediumSoftScore> {

    int hardScore;
    int mediumScore;
    int softScore;

    HardMediumSoftScoreInliner(Map<Constraint, HardMediumSoftScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        var constraintWeight = constraintWeightMap.get(constraint);
        var context = new HardMediumSoftScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context, HardMediumSoftScoreContext::changeScoreBy);
    }

    @Override
    public HardMediumSoftScore extractScore() {
        return HardMediumSoftScore.of(hardScore, mediumScore, softScore);
    }

    @Override
    public String toString() {
        return HardMediumSoftScore.class.getSimpleName() + " inliner";
    }

}
