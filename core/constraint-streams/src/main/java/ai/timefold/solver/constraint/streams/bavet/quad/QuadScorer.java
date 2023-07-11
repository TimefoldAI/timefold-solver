package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.function.PentaFunction;

final class QuadScorer<A, B, C, D> extends AbstractScorer<QuadTuple<A, B, C, D>> {

    private final PentaFunction<A, B, C, D, WeightedScoreImpacter<?, ?>, UndoScoreImpacter> scoreImpacter;

    public QuadScorer(WeightedScoreImpacter<?, ?> weightedScoreImpacter,
            PentaFunction<A, B, C, D, WeightedScoreImpacter<?, ?>, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(weightedScoreImpacter, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(QuadTuple<A, B, C, D> tuple) {
        try {
            return scoreImpacter.apply(tuple.factA, tuple.factB, tuple.factC, tuple.factD, weightedScoreImpacter);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
