package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record QuadLongImpactHandler<A, B, C, D>(ToLongQuadFunction<A, B, C, D> matchWeigher)
        implements
            QuadImpactHandler<A, B, C, D> {

    @Override
    public UndoScoreImpacter impactNaked(WeightedScoreImpacter<?, ?> impacter, QuadTuple<A, B, C, D> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD()), null);
    }

    @Override
    public UndoScoreImpacter impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter,
            QuadTuple<A, B, C, D> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB(), tuple.getC(), tuple.getD()),
                ConstraintMatchSupplier.empty());
    }

    @Override
    public UndoScoreImpacter impactFull(WeightedScoreImpacter<?, ?> impacter, QuadTuple<A, B, C, D> tuple) {
        var a = tuple.getA();
        var b = tuple.getB();
        var c = tuple.getC();
        var d = tuple.getD();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.applyAsLong(a, b, c, d),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                        c, d));
    }

}
