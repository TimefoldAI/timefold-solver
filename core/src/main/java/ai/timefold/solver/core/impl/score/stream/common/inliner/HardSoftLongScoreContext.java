package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardSoftLongScoreContext extends ScoreContext<HardSoftLongScore, HardSoftLongScoreInliner> {

    public HardSoftLongScoreContext(HardSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardSoftLongScore> changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
        var softImpact = constraintWeight.softScore() * matchWeight;
        inliner.softScore += softImpact;
        var scoreImpact = new SoftImpact(inliner, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftLongScore> changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore() * matchWeight;
        inliner.hardScore += hardImpact;
        var scoreImpact = new HardImpact(inliner, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
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
    private record SoftImpact(HardSoftLongScoreInliner inliner,
            long softImpact) implements ScoreImpact<HardSoftLongScore> {

        @Override
        public AbstractScoreInliner<HardSoftLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScore -= softImpact;
        }

        @Override
        public HardSoftLongScore toScore() {
            return HardSoftLongScore.ofSoft(softImpact);
        }

    }

    @NullMarked
    private record HardImpact(HardSoftLongScoreInliner inliner,
            long hardImpact) implements ScoreImpact<HardSoftLongScore> {

        @Override
        public AbstractScoreInliner<HardSoftLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
        }

        @Override
        public HardSoftLongScore toScore() {
            return HardSoftLongScore.ofHard(hardImpact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardSoftLongScoreInliner inliner, long hardImpact,
            long softImpact) implements ScoreImpact<HardSoftLongScore> {

        @Override
        public AbstractScoreInliner<HardSoftLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore -= hardImpact;
            inliner.softScore -= softImpact;
        }

        @Override
        public HardSoftLongScore toScore() {
            return HardSoftLongScore.of(hardImpact, softImpact);
        }

    }

}
