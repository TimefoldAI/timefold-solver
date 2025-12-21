package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ScoreImpact;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record UniLongImpactHandler<A>(ToLongFunction<A> matchWeigher) implements UniImpactHandler<A> {

    @Override
    public ScoreImpact<?> impactNaked(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA()), null);
    }

    @Override
    public ScoreImpact<?> impactFull(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        var a = tuple.getA();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.applyAsLong(a),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a));
    }

    @Override
    public ScoreImpact<?> impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.applyAsLong(tuple.getA()), ConstraintMatchSupplier.empty());
    }

}
