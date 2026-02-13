package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class SimpleScoreContext extends ScoreContext<SimpleScore, SimpleScoreInliner> {

    public SimpleScoreContext(SimpleScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<SimpleScore> changeScoreBy(long matchWeight,
            @Nullable ConstraintMatchSupplier<SimpleScore> constraintMatchSupplier) {
        var impact = Math.multiplyExact(constraintWeight.score(), matchWeight);
        inliner.score = Math.addExact(inliner.score, impact);
        var scoreImpact = new Impact(inliner, impact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record Impact(SimpleScoreInliner inliner, long impact)
            implements
                ScoreImpact<SimpleScore> {

        @Override
        public void undo() {
            inliner.score = Math.subtractExact(inliner.score, impact);
        }

        @Override
        public SimpleScore toScore() {
            return SimpleScore.of(impact);
        }

    }

}
