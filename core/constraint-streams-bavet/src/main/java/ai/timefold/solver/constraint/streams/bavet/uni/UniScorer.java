package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractScorer;
import ai.timefold.solver.constraint.streams.common.inliner.UndoScoreImpacter;
import ai.timefold.solver.core.api.score.Score;

final class UniScorer<A> extends AbstractScorer<UniTuple<A>> {

    private final Function<A, UndoScoreImpacter> scoreImpacter;

    public UniScorer(String constraintPackage, String constraintName, Score<?> constraintWeight,
            Function<A, UndoScoreImpacter> scoreImpacter, int inputStoreIndex) {
        super(constraintPackage, constraintName, constraintWeight, inputStoreIndex);
        this.scoreImpacter = scoreImpacter;
    }

    @Override
    protected UndoScoreImpacter impact(UniTuple<A> tuple) {
        try {
            return scoreImpacter.apply(tuple.getFactA());
        } catch (Exception e) {
            throw createExceptionOnImpact(tuple, e);
        }
    }
}
