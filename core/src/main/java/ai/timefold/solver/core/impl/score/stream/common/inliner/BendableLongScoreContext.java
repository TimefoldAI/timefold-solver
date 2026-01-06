package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class BendableLongScoreContext extends ScoreContext<BendableLongScore, BendableLongScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final long scoreLevelWeight;

    public BendableLongScoreContext(BendableLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableLongScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel,
            long scoreLevelWeight) {
        super(parent, constraint, constraintWeight);
        this.hardScoreLevelCount = hardScoreLevelCount;
        this.softScoreLevelCount = softScoreLevelCount;
        this.scoreLevel = scoreLevel;
        this.scoreLevelWeight = scoreLevelWeight;
    }

    public BendableLongScoreContext(BendableLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableLongScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount) {
        this(parent, constraint, constraintWeight, hardScoreLevelCount, softScoreLevelCount, -1, -1);
    }

    public ScoreImpact<BendableLongScore> changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        var softImpact = scoreLevelWeight * matchWeight;
        inliner.softScores[scoreLevel] += softImpact;
        var scoreImpact = new SingleSoftImpact(this, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableLongScore> changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight * matchWeight;
        inliner.hardScores[scoreLevel] += hardImpact;
        var scoreImpact = new SingleHardImpact(this, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        var hardImpacts = new long[hardScoreLevelCount];
        var softImpacts = new long[softScoreLevelCount];
        for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            var hardImpact = constraintWeight.hardScore(hardScoreLevel) * matchWeight;
            hardImpacts[hardScoreLevel] = hardImpact;
            inliner.hardScores[hardScoreLevel] += hardImpact;
        }
        for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            var softImpact = constraintWeight.softScore(softScoreLevel) * matchWeight;
            softImpacts[softScoreLevel] = softImpact;
            inliner.softScores[softScoreLevel] += softImpact;
        }
        var scoreImpact = new ComplexImpact(this, hardImpacts, softImpacts);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftImpact(BendableLongScoreContext ctx,
            long impact) implements ScoreImpact<BendableLongScore> {

        @Override
        public void undo() {
            ctx.inliner.softScores[ctx.scoreLevel] -= impact;
        }

        @Override
        public BendableLongScore toScore() {
            return BendableLongScore.ofSoft(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardImpact(BendableLongScoreContext ctx,
            long impact) implements ScoreImpact<BendableLongScore> {

        @Override
        public void undo() {
            ctx.inliner.hardScores[ctx.scoreLevel] -= impact;
        }

        @Override
        public BendableLongScore toScore() {
            return BendableLongScore.ofHard(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexImpact(BendableLongScoreContext ctx, long[] hardImpacts,
            long[] softImpacts) implements ScoreImpact<BendableLongScore> {

        @Override
        public void undo() {
            var inliner = ctx.inliner;
            for (var hardScoreLevel = 0; hardScoreLevel < ctx.hardScoreLevelCount; hardScoreLevel++) {
                inliner.hardScores[hardScoreLevel] -= hardImpacts[hardScoreLevel];
            }
            for (var softScoreLevel = 0; softScoreLevel < ctx.softScoreLevelCount; softScoreLevel++) {
                inliner.softScores[softScoreLevel] -= softImpacts[softScoreLevel];
            }
        }

        @Override
        public BendableLongScore toScore() {
            return BendableLongScore.of(hardImpacts, softImpacts);
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
