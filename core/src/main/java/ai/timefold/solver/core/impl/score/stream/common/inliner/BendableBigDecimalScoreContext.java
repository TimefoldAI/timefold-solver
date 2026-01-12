package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class BendableBigDecimalScoreContext
        extends ScoreContext<BendableBigDecimalScore, BendableBigDecimalScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final BigDecimal scoreLevelWeight;

    public BendableBigDecimalScoreContext(BendableBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableBigDecimalScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel,
            BigDecimal scoreLevelWeight) {
        super(parent, constraint, constraintWeight);
        this.hardScoreLevelCount = hardScoreLevelCount;
        this.softScoreLevelCount = softScoreLevelCount;
        this.scoreLevel = scoreLevel;
        this.scoreLevelWeight = scoreLevelWeight;
    }

    public BendableBigDecimalScoreContext(BendableBigDecimalScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableBigDecimalScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount) {
        this(parent, constraint, constraintWeight, hardScoreLevelCount, softScoreLevelCount, -1, BigDecimal.ZERO);
    }

    public ScoreImpact<BendableBigDecimalScore> changeSoftScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var softImpact = scoreLevelWeight.multiply(matchWeight);
        inliner.softScores[scoreLevel] = inliner.softScores[scoreLevel].add(softImpact);
        var scoreImpact = new SingleSoftImpact(this, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableBigDecimalScore> changeHardScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight.multiply(matchWeight);
        inliner.hardScores[scoreLevel] = inliner.hardScores[scoreLevel].add(hardImpact);
        var scoreImpact = new SingleHardImpact(this, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableBigDecimalScore> changeScoreBy(BigDecimal matchWeight,
            ConstraintMatchSupplier<BendableBigDecimalScore> constraintMatchSupplier) {
        var hardImpacts = new BigDecimal[hardScoreLevelCount];
        var softImpacts = new BigDecimal[softScoreLevelCount];
        for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            var hardImpact = constraintWeight.hardScore(hardScoreLevel).multiply(matchWeight);
            hardImpacts[hardScoreLevel] = hardImpact;
            inliner.hardScores[hardScoreLevel] = inliner.hardScores[hardScoreLevel].add(hardImpact);
        }
        for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            var softImpact = constraintWeight.softScore(softScoreLevel).multiply(matchWeight);
            softImpacts[softScoreLevel] = softImpact;
            inliner.softScores[softScoreLevel] = inliner.softScores[softScoreLevel].add(softImpact);
        }
        var scoreImpact = new ComplexImpact(this, hardImpacts, softImpacts);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftImpact(BendableBigDecimalScoreContext ctx,
            BigDecimal impact) implements ScoreImpact<BendableBigDecimalScore> {

        @Override
        public void undo() {
            var inliner = ctx.inliner;
            var level = ctx.scoreLevel;
            inliner.softScores[level] = inliner.softScores[level].subtract(impact);
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.ofSoft(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardImpact(BendableBigDecimalScoreContext ctx,
            BigDecimal impact) implements ScoreImpact<BendableBigDecimalScore> {

        @Override
        public void undo() {
            var inliner = ctx.inliner;
            var level = ctx.scoreLevel;
            inliner.hardScores[level] = inliner.hardScores[level].subtract(impact);
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.ofHard(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexImpact(BendableBigDecimalScoreContext ctx, BigDecimal[] hardImpacts,
            BigDecimal[] softImpacts) implements ScoreImpact<BendableBigDecimalScore> {

        @Override
        public void undo() {
            var inliner = ctx.inliner;
            for (var hardScoreLevel = 0; hardScoreLevel < ctx.hardScoreLevelCount; hardScoreLevel++) {
                inliner.hardScores[hardScoreLevel] = inliner.hardScores[hardScoreLevel].subtract(hardImpacts[hardScoreLevel]);
            }
            for (var softScoreLevel = 0; softScoreLevel < ctx.softScoreLevelCount; softScoreLevel++) {
                inliner.softScores[softScoreLevel] = inliner.softScores[softScoreLevel].subtract(softImpacts[softScoreLevel]);
            }
        }

        @Override
        public BendableBigDecimalScore toScore() {
            return BendableBigDecimalScore.of(hardImpacts, softImpacts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ComplexImpact that)) {
                return false;
            }
            return Objects.equals(ctx, that.ctx) &&
                    Objects.deepEquals(hardImpacts, that.hardImpacts) &&
                    Objects.deepEquals(softImpacts, that.softImpacts);
        }

        @Override
        public int hashCode() {
            var hash = 1;
            hash = 31 * hash + ctx.hashCode();
            hash = 31 * hash + Arrays.hashCode(hardImpacts);
            hash = 31 * hash + Arrays.hashCode(softImpacts);
            return hash;
        }

        @Override
        public String toString() {
            return "Impact(hard: %s, soft: %s)"
                    .formatted(Arrays.toString(hardImpacts), Arrays.toString(softImpacts));
        }

    }

}
