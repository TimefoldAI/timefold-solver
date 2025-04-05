package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class SimpleLongScoreInliner extends AbstractScoreInliner<SimpleLongScore> {

    long score;

    SimpleLongScoreInliner(Map<Constraint, SimpleLongScore> constraintWeightMap, ConstraintMatchPolicy constraintMatchPolicy) {
        super(constraintWeightMap, constraintMatchPolicy);
    }

    @Override
    public WeightedScoreImpacter<SimpleLongScore, ?> buildWeightedScoreImpacter(
            AbstractConstraint<?, ?, ?> constraint) {
        SimpleLongScore constraintWeight = constraintWeightMap.get(constraint);
        SimpleLongScoreContext context = new SimpleLongScoreContext(this, constraint, constraintWeight);
        return WeightedScoreImpacter.of(context,
                (SimpleLongScoreContext ctx, long matchWeight,
                        ConstraintMatchSupplier<SimpleLongScore> constraintMatchSupplier) -> ctx
                                .changeScoreBy(matchWeight, constraintMatchSupplier));
    }

    @Override
    public SimpleLongScore extractScore() {
        return SimpleLongScore.of(score);
    }

    @Override
    public String toString() {
        return SimpleLongScore.class.getSimpleName() + " inliner";
    }

}
