package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.ToLongBiFunction;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record BiLongImpactHandler<A, B>(ToLongBiFunction<A, B> matchWeigher)
        implements
            BiImpactHandler<A, B> {

    @Override
    public UndoScoreImpacter impactNaked(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB()), null);
    }

    @Override
    public UndoScoreImpacter impactFull(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        var a = tuple.getA();
        var b = tuple.getB();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.applyAsLong(a, b),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a, b));
    }

    @Override
    public UndoScoreImpacter impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, BiTuple<A, B> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA(), tuple.getB()), ConstraintMatchSupplier.empty());
    }

}
