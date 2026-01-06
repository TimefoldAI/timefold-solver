package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter.LongImpactFunction;

final class HardMediumSoftLongScoreInliner extends AbstractScoreInliner<HardMediumSoftLongScore> {

    long hardScore;
    long mediumScore;
    long softScore;

    HardMediumSoftLongScoreInliner(Map<Constraint, HardMediumSoftLongScore> constraintWeightMap,
            ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftLongScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        var constraintWeight = constraintWeightMap.get(constraint);
        var softConstraintWeight = constraintWeight.softScore();
        var mediumConstraintWeight = constraintWeight.mediumScore();
        var hardConstraintWeight = constraintWeight.hardScore();
        var context = new HardMediumSoftLongScoreContext(this, constraint, constraintWeight);
        if (mediumConstraintWeight == 0 && softConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardMediumSoftLongScore, HardMediumSoftLongScoreContext>) HardMediumSoftLongScoreContext::changeHardScoreBy);
        } else if (hardConstraintWeight == 0 && softConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardMediumSoftLongScore, HardMediumSoftLongScoreContext>) HardMediumSoftLongScoreContext::changeMediumScoreBy);
        } else if (hardConstraintWeight == 0 && mediumConstraintWeight == 0) {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardMediumSoftLongScore, HardMediumSoftLongScoreContext>) HardMediumSoftLongScoreContext::changeSoftScoreBy);
        } else {
            return WeightedScoreImpacter.of(context,
                    (LongImpactFunction<HardMediumSoftLongScore, HardMediumSoftLongScoreContext>) HardMediumSoftLongScoreContext::changeScoreBy);
        }
    }

    @Override
    public HardMediumSoftLongScore extractScore() {
        return HardMediumSoftLongScore.of(hardScore, mediumScore, softScore);
    }

    @Override
    public String toString() {
        return HardMediumSoftLongScore.class.getSimpleName() + " inliner";
    }

}
