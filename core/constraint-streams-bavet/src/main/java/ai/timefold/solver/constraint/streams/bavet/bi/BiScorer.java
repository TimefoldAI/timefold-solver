package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.api.score.Score;

final class BiScorer<A, B> extends AbstractScorer<BiTuple<A, B>> {

    private final BiFunction<A, B, UndoScoreImpacter> scoreImpacter;

    public BiScorer(String constraintPackage, String constraintName, Score<?> constraintWeight,
            BiFunction<A, B, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(constraintPackage, constraintName, constraintWeight, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(BiTuple<A, B> tuple) {
        try {
            return scoreImpacter.apply(tuple.getFactA(), tuple.getFactB());
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
