package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardSoftScoreContext extends ScoreContext<HardSoftScore, HardSoftScoreInliner> {

    public HardSoftScoreContext(HardSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardSoftScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardSoftScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        var softImpact = constraintWeight.softScore() * matchWeight;
        inliner.hardScore += hardImpact;
        inliner.softScore += softImpact;
        var scoreImpact = new ComplexImpact(inliner, hardImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record ComplexImpact(HardSoftScoreInliner inliner, int hardImpact,
            int softImpact) implements ScoreImpact<HardSoftScore> {

        @Override
        public AbstractScoreInliner<HardSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
            inliner.softScore -= softImpact;
        }

        @Override
        public HardSoftScore toScore() {
            return HardSoftScore.of(hardImpact, softImpact);
        }

    }

}
