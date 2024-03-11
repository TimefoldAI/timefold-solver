package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftLongScoreInliner extends AbstractScoreInliner<HardMediumSoftLongScore> {

    long hardScore;
    long mediumScore;
    long softScore;

    HardMediumSoftLongScoreInliner(Map<Constraint, HardMediumSoftLongScore> constraintWeightMap,
            boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardMediumSoftLongScore, ?>
            buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        HardMediumSoftLongScore constraintWeight = constraintWeightMap.get(constraint);
        long hardConstraintWeight = constraintWeight.hardScore();
        long mediumConstraintWeight = constraintWeight.mediumScore();
        long softConstraintWeight = constraintWeight.softScore();
        HardMediumSoftLongScoreContext context = new HardMediumSoftLongScoreContext(this, constraint, constraintWeight);
        if (mediumConstraintWeight == 0L && softConstraintWeight == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardMediumSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeHardScoreBy(matchWeight, constraintMatchSupplier));
        } else if (hardConstraintWeight == 0L && softConstraintWeight == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardMediumSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeMediumScoreBy(matchWeight, constraintMatchSupplier));
        } else if (hardConstraintWeight == 0L && mediumConstraintWeight == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardMediumSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeSoftScoreBy(matchWeight, constraintMatchSupplier));
        } else {
            return WeightedScoreImpacter.of(context,
                    (HardMediumSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeScoreBy(matchWeight, constraintMatchSupplier));
        }
    }

    @Override
    public HardMediumSoftLongScore extractScore(int initScore) {
        return HardMediumSoftLongScore.ofUninitialized(initScore, hardScore, mediumScore, softScore);
    }

    @Override
    public String toString() {
        return HardMediumSoftLongScore.class.getSimpleName() + " inliner";
    }

}
