package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.constraint.streams.common.inliner.WeightedScoreImpacter;
import ai.timefold.solver.core.api.function.QuadFunction;

final class TriScorer<A, B, C> extends AbstractScorer<TriTuple<A, B, C>> {

    private final QuadFunction<WeightedScoreImpacter<?, ?>, A, B, C, UndoScoreImpacter> scoreImpacter;

    public TriScorer(WeightedScoreImpacter<?, ?> weightedScoreImpacter,
            QuadFunction<WeightedScoreImpacter<?, ?>, A, B, C, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(weightedScoreImpacter, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(TriTuple<A, B, C> tuple) {
        try {
            return scoreImpacter.apply(weightedScoreImpacter, tuple.factA, tuple.factB, tuple.factC);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
