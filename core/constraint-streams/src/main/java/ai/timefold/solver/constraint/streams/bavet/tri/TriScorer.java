package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;

final class TriScorer<A, B, C> extends AbstractScorer<TriTuple<A, B, C>> {

    private final TriFunction<A, B, C, UndoScoreImpacter> scoreImpacter;

    public TriScorer(String constraintPackage, String constraintName, Score<?> constraintWeight,
            TriFunction<A, B, C, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(constraintPackage, constraintName, constraintWeight, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(TriTuple<A, B, C> tuple) {
        try {
            return scoreImpacter.apply(tuple.factA, tuple.factB, tuple.factC);
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
