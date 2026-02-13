package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BendableScoreContext extends ScoreContext<BendableScore, BendableScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final long scoreLevelWeight;

    public BendableScoreContext(BendableScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel,
            long scoreLevelWeight) {
        super(parent, constraint, constraintWeight);
        this.hardScoreLevelCount = hardScoreLevelCount;
        this.softScoreLevelCount = softScoreLevelCount;
        this.scoreLevel = scoreLevel;
        this.scoreLevelWeight = scoreLevelWeight;
    }

    public BendableScoreContext(BendableScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount) {
        this(parent, constraint, constraintWeight, hardScoreLevelCount, softScoreLevelCount, -1, -1);
    }

    public ScoreImpact<BendableScore> changeSoftScoreBy(long matchWeight,
            @Nullable ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var softImpact = Math.multiplyExact(scoreLevelWeight, matchWeight);
        inliner.softScores[scoreLevel] = Math.addExact(inliner.softScores[scoreLevel], softImpact);
        var scoreImpact = new SingleSoftImpact(this, softImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableScore> changeHardScoreBy(long matchWeight,
            @Nullable ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var hardImpact = Math.multiplyExact(scoreLevelWeight, matchWeight);
        inliner.hardScores[scoreLevel] = Math.addExact(inliner.hardScores[scoreLevel], hardImpact);
        var scoreImpact = new SingleHardImpact(this, hardImpact);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableScore> changeScoreBy(long matchWeight,
            @Nullable ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var hardImpacts = new long[hardScoreLevelCount];
        var softImpacts = new long[softScoreLevelCount];
        for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            var hardImpact = Math.multiplyExact(constraintWeight.hardScore(hardScoreLevel), matchWeight);
            hardImpacts[hardScoreLevel] = hardImpact;
            inliner.hardScores[hardScoreLevel] = Math.addExact(inliner.hardScores[hardScoreLevel], hardImpact);
        }
        for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            var softImpact = Math.multiplyExact(constraintWeight.softScore(softScoreLevel), matchWeight);
            softImpacts[softScoreLevel] = softImpact;
            inliner.softScores[softScoreLevel] = Math.addExact(inliner.softScores[softScoreLevel], softImpact);
        }
        var scoreImpact = new ComplexImpact(this, hardImpacts, softImpacts);
        return possiblyAddConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftImpact(BendableScoreContext ctx, long impact)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public void undo() {
            ctx.inliner.softScores[ctx.scoreLevel] = Math.subtractExact(ctx.inliner.softScores[ctx.scoreLevel], impact);
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofSoft(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardImpact(BendableScoreContext ctx, long impact)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public void undo() {
            ctx.inliner.hardScores[ctx.scoreLevel] = Math.subtractExact(ctx.inliner.hardScores[ctx.scoreLevel], impact);
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofHard(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexImpact(BendableScoreContext ctx, long[] hardImpacts, long[] softImpacts)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public void undo() {
            var inliner = ctx.inliner;
            for (var hardScoreLevel = 0; hardScoreLevel < ctx.hardScoreLevelCount; hardScoreLevel++) {
                inliner.hardScores[hardScoreLevel] =
                        Math.subtractExact(inliner.hardScores[hardScoreLevel], hardImpacts[hardScoreLevel]);
            }
            for (var softScoreLevel = 0; softScoreLevel < ctx.softScoreLevelCount; softScoreLevel++) {
                inliner.softScores[softScoreLevel] =
                        Math.subtractExact(inliner.softScores[softScoreLevel], softImpacts[softScoreLevel]);
            }
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.of(hardImpacts, softImpacts);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ComplexImpact(BendableScoreContext otherCtx, long[] otherHardImpacts, long[] otherSoftImpacts))) {
                return false;
            }
            return Objects.equals(ctx, otherCtx) && Objects.deepEquals(hardImpacts, otherHardImpacts)
                    && Objects.deepEquals(softImpacts, otherSoftImpacts);
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
            return "Impact(hard: %s, soft: %s)".formatted(Arrays.toString(hardImpacts), Arrays.toString(softImpacts));
        }

    }

}
