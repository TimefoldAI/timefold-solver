package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class HardSoftScoreInliner extends AbstractScoreInliner<HardSoftScore> {

    long hardScore;
    long softScore;

    HardSoftScoreInliner(Map<Constraint, HardSoftScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<HardSoftScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        var constraintWeight = constraintWeightMap.get(constraint);
        var context = new HardSoftScoreContext(this, constraint, constraintWeight);
        if (constraintWeight.softScore() == 0) {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeHardScoreBy);
        } else if (constraintWeight.hardScore() == 0) {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context, HardSoftScoreContext::changeScoreBy);
        }
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
