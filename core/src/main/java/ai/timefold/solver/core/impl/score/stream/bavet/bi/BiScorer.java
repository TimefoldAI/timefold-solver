package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractScorer;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.impl.score.stream.common.inliner.WeightedScoreImpacter;

final class BiScorer<A, B> extends AbstractScorer<BiTuple<A, B>> {

    private final TriFunction<WeightedScoreImpacter<?, ?>, A, B, UndoScoreImpacter> scoreImpacter;

    public BiScorer(WeightedScoreImpacter<?, ?> weightedScoreImpacter,
            TriFunction<WeightedScoreImpacter<?, ?>, A, B, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(weightedScoreImpacter, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(BiTuple<A, B> tuple) {
        try {
            return scoreImpacter.apply(weightedScoreImpacter, tuple.factA, tuple.factB);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
