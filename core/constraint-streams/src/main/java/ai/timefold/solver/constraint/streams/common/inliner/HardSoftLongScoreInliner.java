package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class HardSoftLongScoreInliner extends AbstractScoreInliner<HardSoftLongScore> {

    private long hardScore;
    private long softScore;

    HardSoftLongScoreInliner(boolean constraintMatchEnabled) {
        super(constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<HardSoftLongScore, HardSoftLongScoreContext> buildWeightedScoreImpacter(Constraint constraint,
            HardSoftLongScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        HardSoftLongScoreContext context =
                new HardSoftLongScoreContext(this, constraint, constraintWeight,
                        impact -> this.hardScore += impact, impact -> this.softScore += impact);
        if (constraintWeight.softScore() == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight, JustificationsSupplier justificationsSupplier) -> ctx
                            .changeHardScoreBy(matchWeight, justificationsSupplier));
        } else if (constraintWeight.hardScore() == 0L) {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight, JustificationsSupplier justificationsSupplier) -> ctx
                            .changeSoftScoreBy(matchWeight, justificationsSupplier));
        } else {
            return WeightedScoreImpacter.of(context,
                    (HardSoftLongScoreContext ctx, long matchWeight, JustificationsSupplier justificationsSupplier) -> ctx
                            .changeScoreBy(matchWeight, justificationsSupplier));
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
