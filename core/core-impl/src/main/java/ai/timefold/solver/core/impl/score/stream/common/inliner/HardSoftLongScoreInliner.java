package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftLongScoreInliner extends AbstractScoreInliner<HardSoftLongScore> {

    long hardScore;
    long softScore;

    HardSoftLongScoreInliner(Map<Constraint, HardSoftLongScore> constraintWeightMap, boolean constraintMatchEnabled) {
        super(constraintWeightMap, constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardSoftLongScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        HardSoftLongScore constraintWeight = constraintWeightMap.get(constraint);
        HardSoftLongScoreContext context = new HardSoftLongScoreContext(this, constraint, constraintWeight);
        if (constraintWeight.softScore() == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeHardScoreBy(matchWeight, constraintMatchSupplier));
        } else if (constraintWeight.hardScore() == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeSoftScoreBy(matchWeight, constraintMatchSupplier));
        } else {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight,
                            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) -> ctx
                                    .changeScoreBy(matchWeight, constraintMatchSupplier));
        }
    }

    @Override
    public HardSoftLongScore extractScore(int initScore) {
        return HardSoftLongScore.ofUninitialized(initScore, hardScore, softScore);
    }

    @Override
    public String toString() {
        return HardSoftLongScore.class.getSimpleName() + " inliner";
    }

}
