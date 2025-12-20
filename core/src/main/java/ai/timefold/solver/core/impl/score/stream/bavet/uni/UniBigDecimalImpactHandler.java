package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.math.BigDecimal;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record UniBigDecimalImpactHandler<A>(Function<A, BigDecimal> matchWeigher) implements UniImpactHandler<A> {

    @Override
    public UndoScoreImpacter impactNaked(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA()), null);
    }

    @Override
    public UndoScoreImpacter impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.apply(tuple.getA()), ConstraintMatchSupplier.empty());
    }

    @Override
    public UndoScoreImpacter impactFull(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        var a = tuple.getA();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.apply(a),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a));
    }

}
