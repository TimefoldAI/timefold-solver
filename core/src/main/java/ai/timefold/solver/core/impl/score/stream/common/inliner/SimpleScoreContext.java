package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class SimpleScoreContext extends ScoreContext<SimpleScore, SimpleScoreInliner> {

    public SimpleScoreContext(SimpleScoreInliner parent, AbstractConstraint<?, ?, ?> constraint, SimpleScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<SimpleScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<SimpleScore> constraintMatchSupplier) {
        var impact = constraintWeight.score() * matchWeight;
        inliner.score += impact;
        var scoreImpact = new Impact(inliner, impact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record Impact(SimpleScoreInliner inliner, int impact) implements ScoreImpact<SimpleScore> {

        @Override
        public void undo() {
            inliner.score -= impact;
        }

        @Override
        public SimpleScore toScore() {
            return SimpleScore.of(impact);
        }

    }

}
