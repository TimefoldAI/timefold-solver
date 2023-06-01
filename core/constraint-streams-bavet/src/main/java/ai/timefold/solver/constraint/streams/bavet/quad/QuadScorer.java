package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;

final class QuadScorer<A, B, C, D> extends AbstractScorer<QuadTuple<A, B, C, D>> {

    private final QuadFunction<A, B, C, D, UndoScoreImpacter> scoreImpacter;

    public QuadScorer(String constraintPackage, String constraintName, Score<?> constraintWeight,
            QuadFunction<A, B, C, D, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(constraintPackage, constraintName, constraintWeight, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(QuadTuple<A, B, C, D> tuple) {
        try {
            return scoreImpacter.apply(tuple.factA, tuple.factB, tuple.factC, tuple.factD);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
