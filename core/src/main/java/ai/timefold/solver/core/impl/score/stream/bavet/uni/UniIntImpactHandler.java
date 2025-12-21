package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ConstraintMatchSupplier;
import ai.timefold.solver.core.impl.score.stream.common.inliner.ScoreImpact;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

import org.jspecify.annotations.NullMarked;

@NullMarked
record UniIntImpactHandler<A>(ToIntFunction<A> matchWeigher) implements UniImpactHandler<A> {

    @Override
    public ScoreImpact<?> impactNaked(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.applyAsInt(tuple.getA()), null);
    }

    @Override
    public ScoreImpact<?> impactWithoutJustification(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        return impacter.impactScore(matchWeigher.applyAsInt(tuple.getA()), ConstraintMatchSupplier.empty());
    }

    @Override
    public ScoreImpact<?> impactFull(WeightedScoreImpacter<?, ?> impacter, UniTuple<A> tuple) {
        var a = tuple.getA();
        var constraint = impacter.getContext().getConstraint();
        return impacter.impactScore(matchWeigher.applyAsInt(a),
                ConstraintMatchSupplier.of(constraint.getJustificationMapping(), constraint.getIndictedObjectsMapping(), a));
    }

}
