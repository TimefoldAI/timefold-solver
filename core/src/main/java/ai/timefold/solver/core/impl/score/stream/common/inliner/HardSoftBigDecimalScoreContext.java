package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class HardSoftBigDecimalScoreContext extends ScoreContext<HardSoftBigDecimalScore, HardSoftBigDecimalScoreInliner> {

    public HardSoftBigDecimalScoreContext(HardSoftBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public ScoreImpact<HardSoftBigDecimalScore> changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var softImpact = constraintWeight.softScore().multiply(matchWeight);
        inliner.softScore = inliner.softScore.add(softImpact);
        var scoreImpact = new SoftImpact(inliner, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftBigDecimalScore> changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        inliner.hardScore = inliner.hardScore.add(hardImpact);
        var scoreImpact = new HardImpact(inliner, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<HardSoftBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<HardSoftBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        var softImpact = constraintWeight.softScore().multiply(matchWeight);
        inliner.hardScore = inliner.hardScore.add(hardImpact);
        inliner.softScore = inliner.softScore.add(softImpact);
        var scoreImpact = new ComplexImpact(inliner, hardImpact, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SoftImpact(HardSoftBigDecimalScoreInliner inliner,
            BigDecimal impact) implements ScoreImpact<HardSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScore = inliner.softScore.subtract(impact);
        }

        @Override
        public HardSoftBigDecimalScore toScore() {
            return HardSoftBigDecimalScore.ofSoft(impact);
        }

    }

    @NullMarked
    private record HardImpact(HardSoftBigDecimalScoreInliner inliner,
            BigDecimal impact) implements ScoreImpact<HardSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore = inliner.hardScore.subtract(impact);
        }

        @Override
        public HardSoftBigDecimalScore toScore() {
            return HardSoftBigDecimalScore.ofHard(impact);
        }

    }

    @NullMarked
    private record ComplexImpact(HardSoftBigDecimalScoreInliner inliner, BigDecimal hardImpact,
            BigDecimal softImpact) implements ScoreImpact<HardSoftBigDecimalScore> {

        @Override
        public AbstractScoreInliner<HardSoftBigDecimalScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScore = inliner.hardScore.subtract(hardImpact);
            inliner.softScore = inliner.softScore.subtract(softImpact);
        }

        @Override
        public HardSoftBigDecimalScore toScore() {
            return HardSoftBigDecimalScore.of(hardImpact, softImpact);
        }

    }

}
