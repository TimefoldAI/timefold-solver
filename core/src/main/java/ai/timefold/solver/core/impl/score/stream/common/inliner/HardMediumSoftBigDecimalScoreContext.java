package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardMediumSoftBigDecimalScoreContext
        extends ScoreContext<HardMediumSoftBigDecimalScore, HardMediumSoftBigDecimalScoreInliner> {

    public HardMediumSoftBigDecimalScoreContext(HardMediumSoftBigDecimalScoreInliner parent,
            AbstractConstraint<?, ?, ?> constraint, HardMediumSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var softImpact = constraintWeight.softScore().multiply(matchWeight);
        parent.softScore = parent.softScore.add(softImpact);
        var scoreImpact = new SoftBigDecimalImpact(parent, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeMediumScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var mediumImpact = constraintWeight.mediumScore().multiply(matchWeight);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        var scoreImpact = new MediumBigDecimalImpact(parent, mediumImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        var scoreImpact = new HardBigDecimalImpact(parent, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        var mediumImpact = constraintWeight.mediumScore().multiply(matchWeight);
        var softImpact = constraintWeight.softScore().multiply(matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        parent.softScore = parent.softScore.add(softImpact);
        var scoreImpact = new HardMediumSoftBigDecimalImpact(parent, hardImpact, mediumImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftBigDecimalImpact(HardMediumSoftBigDecimalScoreInliner inliner, BigDecimal impact)
            implements
                ScoreImpact<HardMediumSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScore = inliner.softScore.subtract(impact);
        }

        @Override
        public HardMediumSoftBigDecimalScore toScore() {
            return HardMediumSoftBigDecimalScore.ofSoft(impact);
        }

    }

    @NullMarked
    private record MediumBigDecimalImpact(HardMediumSoftBigDecimalScoreInliner inliner, BigDecimal impact)
            implements
                ScoreImpact<HardMediumSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.mediumScore = inliner.mediumScore.subtract(impact);
        }

        @Override
        public HardMediumSoftBigDecimalScore toScore() {
            return HardMediumSoftBigDecimalScore.ofMedium(impact);
        }

    }

    @NullMarked
    private record HardBigDecimalImpact(HardMediumSoftBigDecimalScoreInliner inliner, BigDecimal impact)
            implements
                ScoreImpact<HardMediumSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore = inliner.hardScore.subtract(impact);
        }

        @Override
        public HardMediumSoftBigDecimalScore toScore() {
            return HardMediumSoftBigDecimalScore.ofHard(impact);
        }

    }

    @NullMarked
    private record HardMediumSoftBigDecimalImpact(HardMediumSoftBigDecimalScoreInliner inliner, BigDecimal hardImpact,
            BigDecimal mediumImpact, BigDecimal softImpact)
            implements
                ScoreImpact<HardMediumSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardMediumSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore = inliner.hardScore.subtract(hardImpact);
            inliner.mediumScore = inliner.mediumScore.subtract(mediumImpact);
            inliner.softScore = inliner.softScore.subtract(softImpact);
        }

        @Override
        public HardMediumSoftBigDecimalScore toScore() {
            return HardMediumSoftBigDecimalScore.of(hardImpact, mediumImpact, softImpact);
        }

    }

}
