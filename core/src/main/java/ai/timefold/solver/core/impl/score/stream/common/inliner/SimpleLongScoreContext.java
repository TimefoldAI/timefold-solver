package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class SimpleLongScoreContext extends ScoreContext<SimpleLongScore, SimpleLongScoreInliner> {

    public SimpleLongScoreContext(SimpleLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            SimpleLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<SimpleLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<SimpleLongScore> constraintMatchSupplier) {
        var impact = constraintWeight.score() * matchWeight;
        inliner.score += impact;
        var scoreImpact = new Impact(inliner, impact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record Impact(SimpleLongScoreInliner inliner, long impact) implements ScoreImpact<SimpleLongScore> {

        @Override
        public void undo() {
            inliner.score -= impact;
        }

        @Override
        public SimpleLongScore toScore() {
            return SimpleLongScore.of(impact);
        }

    }

}
