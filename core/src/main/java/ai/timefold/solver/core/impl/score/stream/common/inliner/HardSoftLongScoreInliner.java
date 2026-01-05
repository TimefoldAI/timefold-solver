package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter.LongImpactFunction;

final class HardSoftLongScoreInliner extends AbstractScoreInliner<HardSoftLongScore> {

    long hardScore;
    long softScore;

    HardSoftLongScoreInliner(Map<Constraint, HardSoftLongScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<HardSoftLongScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        var constraintWeight = constraintWeightMap.get(constraint);
        var softConstraintWeight = constraintWeight.softScore();
        var hardConstraintWeight = constraintWeight.hardScore();
        var context = new HardSoftLongScoreContext(this, constraint, constraintWeight);
        if (softConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardSoftLongScore, HardSoftLongScoreContext>) HardSoftLongScoreContext::changeHardScoreBy);
        } else if (hardConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardSoftLongScore, HardSoftLongScoreContext>) HardSoftLongScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardSoftLongScore, HardSoftLongScoreContext>) HardSoftLongScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardSoftLongScore extractScore() {
        return HardSoftLongScore.of(hardScore, softScore);
    }

    @Override
    public String toString() {
        return HardSoftLongScore.class.getSimpleName() + " inliner";
    }

}
