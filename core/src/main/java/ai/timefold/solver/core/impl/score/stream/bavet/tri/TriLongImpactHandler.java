package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.ToLongTriFunction;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record TriLongImpactHandler<A, B, C>(ToLongTriFunction<A, B, C> matchWeigher)
        implements
            TriImpactHandler<A, B, C> {

    @Override
    public UndoScoreImpacter impactNaked(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB(), tuple.getC()), null);
    }

    @Override
    public UndoScoreImpacter impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB(), tuple.getC()),
                ConstraintMatchSupplier.empty());
    }

    @Override
    public UndoScoreImpacter impactFull(WeightedScoreImpacter<?, ?> impacter, TriTuple<A, B, C> tuple) {
        var a = tuple.getA();
        B b = tuple.getB();
        C c = tuple.getC();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.applyAsLong(a, b, c),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b,
                        c));
    }

}
