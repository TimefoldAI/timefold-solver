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
        inliner.softScore = inliner.softScore.add(softImpact);
        var scoreImpact = new SoftImpact(inliner, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeMediumScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var mediumImpact = constraintWeight.mediumScore().multiply(matchWeight);
        inliner.mediumScore = inliner.mediumScore.add(mediumImpact);
        var scoreImpact = new MediumImpact(inliner, mediumImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardMediumSoftBigDecimalScore> changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardMediumSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        inliner.hardScore = inliner.hardScore.add(hardImpact);
        var scoreImpact = new HardImpact(inliner, hardImpact);
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
        inliner.hardScore = inliner.hardScore.add(hardImpact);
        inliner.mediumScore = inliner.mediumScore.add(mediumImpact);
        inliner.softScore = inliner.softScore.add(softImpact);
        var scoreImpact = new ComplexImpact(inliner, hardImpact, mediumImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftImpact(HardMediumSoftBigDecimalScoreInliner inliner,
            BigDecimal impact) implements ScoreImpact<HardMediumSoftBigDecimalScore> {

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
    private record MediumImpact(HardMediumSoftBigDecimalScoreInliner inliner,
            BigDecimal impact) implements ScoreImpact<HardMediumSoftBigDecimalScore> {

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
    private record HardImpact(HardMediumSoftBigDecimalScoreInliner inliner,
            BigDecimal impact) implements ScoreImpact<HardMediumSoftBigDecimalScore> {

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
    private record ComplexImpact(HardMediumSoftBigDecimalScoreInliner inliner, BigDecimal hardImpact,
            BigDecimal mediumImpact,
            BigDecimal softImpact) implements ScoreImpact<HardMediumSoftBigDecimalScore> {

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
