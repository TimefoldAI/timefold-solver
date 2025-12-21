package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftScoreContext extends ScoreContext<HardMediumSoftScore, HardMediumSoftScoreInliner> {

    public HardMediumSoftScoreContext(HardMediumSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<HardMediumSoftScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        var mediumImpact = constraintWeight.mediumScore() * matchWeight;
        var softImpact = constraintWeight.softScore() * matchWeight;
        parent.hardScore += hardImpact;
        parent.mediumScore += mediumImpact;
        parent.softScore += softImpact;
        var scoreImpact = new HardMediumSoftImpact(parent, hardImpact, mediumImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record HardMediumSoftImpact(HardMediumSoftScoreInliner inliner, int hardImpact, int mediumImpact, int softImpact)
            implements
                ScoreImpact<HardMediumSoftScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
            inliner.mediumScore -= mediumImpact;
            inliner.softScore -= softImpact;
        }

        @Override
        public HardMediumSoftScore toScore() {
            return HardMediumSoftScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
