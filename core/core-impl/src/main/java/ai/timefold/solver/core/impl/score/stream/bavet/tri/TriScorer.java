package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractScorer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

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
