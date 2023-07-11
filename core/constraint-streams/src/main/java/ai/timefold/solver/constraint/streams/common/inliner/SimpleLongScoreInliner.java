package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;

final class SimpleLongScoreInliner extends AbstractScoreInliner<SimpleLongScore> {

    long score;

    SimpleLongScoreInliner(boolean constraintMatchEnabled) {
        super(constraintMatchEnabled);
    }

    @Override
    public WeightedScoreImpacter<SimpleLongScoreContext> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint,
            SimpleLongScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        SimpleLongScoreContext context = new SimpleLongScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context,
                (SimpleLongScoreContext ctx, long matchWeight, JustificationsSupplier justificationsSupplier) -> ctx
                        .changeScoreBy(matchWeight, justificationsSupplier));
    }

    @Override
    public SimpleLongScore extractScore(int initScore) {
        return SimpleLongScore.ofUninitialized(initScore, score);
    }

    @Override
    public String toString() {
        return SimpleLongScore.class.getSimpleName() + " inliner";
    }

}
