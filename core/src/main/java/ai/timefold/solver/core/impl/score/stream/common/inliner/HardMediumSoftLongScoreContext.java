package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftLongScoreContext extends ScoreContext<HardMediumSoftLongScore, HardMediumSoftLongScoreInliner> {

    public HardMediumSoftLongScoreContext(HardMediumSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        var mediumImpact = constraintWeight.mediumScore() * matchWeight;
        var softImpact = constraintWeight.softScore() * matchWeight;
        parent.hardScore += hardImpact;
        parent.mediumScore += mediumImpact;
        parent.softScore += softImpact;
        var scoreImpact = new HardMediumSoftLongImpact(parent, hardImpact, mediumImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record HardMediumSoftLongImpact(HardMediumSoftLongScoreInliner inliner, long hardImpact, long mediumImpact,
            long softImpact)
            implements
                ScoreImpact<HardMediumSoftLongScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
            inliner.mediumScore -= mediumImpact;
            inliner.softScore -= softImpact;
        }

        @Override
        public HardMediumSoftLongScore toScore() {
            return HardMediumSoftLongScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
