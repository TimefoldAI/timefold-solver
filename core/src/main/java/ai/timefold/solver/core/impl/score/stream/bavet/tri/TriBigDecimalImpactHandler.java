package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record TriBigDecimalImpactHandler<A, B, C>(TriFunction<A, B, C, BigDecimal> matchWeigher)
        implements
            TriImpactHandler<A, B, C> {

    @Override
    public UndoScoreImpacter impactNaked(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA(), tuple.getB(), tuple.getC()), null);
    }

    @Override
    public UndoScoreImpacter impactFull(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        var a = tuple.getA();
        var b = tuple.getB();
        var c = tuple.getC();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.apply(a, b, c),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                        c));
    }

    @Override
    public UndoScoreImpacter impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA(), tuple.getB(), tuple.getC()),
                ConstraintMatchSupplier.empty());
    }

}
