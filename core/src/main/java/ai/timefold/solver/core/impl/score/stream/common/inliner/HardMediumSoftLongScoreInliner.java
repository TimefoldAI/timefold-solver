package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

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
        var context = new HardMediumSoftLongScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context,
                (HardMediumSoftLongScoreContext ctx, long matchWeight,
                        ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) -> ctx
                                .changeScoreBy(matchWeight, constraintMatchSupplier));
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
