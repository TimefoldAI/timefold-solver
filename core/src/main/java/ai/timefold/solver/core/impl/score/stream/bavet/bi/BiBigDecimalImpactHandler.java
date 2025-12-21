package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.math.BigDecimal;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ScoreImpact;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record BiBigDecimalImpactHandler<A, B>(BiFunction<A, B, BigDecimal> matchWeigher)
        implements
            BiImpactHandler<A, B> {

    @Override
    public ScoreImpact<?> impactNaked(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA(), tuple.getB()), null);
    }

    @Override
    public ScoreImpact<?> impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA(), tuple.getB()), ConstraintMatchSupplier.empty());
    }

    @Override
    public ScoreImpact<?> impactFull(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        var a = tuple.getA();
        var b = tuple.getB();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.apply(a, b),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b));
    }

}
