package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

import org.jspecify.annotations.NullMarked;

final class BendableScoreContext extends ScoreContext<BendableScore, BendableScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final int scoreLevelWeight;

    public BendableScoreContext(BendableScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            BendableScore constraintWeight, int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel,
            int scoreLevelWeight) {
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

    public ScoreImpact<BendableScore> changeSoftScoreBy(int matchWeight,
            ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var softImpact = scoreLevelWeight * matchWeight;
        inliner.softScores[scoreLevel] += softImpact;
        var scoreImpact = new SingleSoftImpact(this, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableScore> changeHardScoreBy(int matchWeight,
            ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight * matchWeight;
        inliner.hardScores[scoreLevel] += hardImpact;
        var scoreImpact = new SingleHardImpact(this, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableScore> changeScoreBy(int matchWeight,
            ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var hardImpacts = new int[hardScoreLevelCount];
        var softImpacts = new int[softScoreLevelCount];
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
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftImpact(BendableScoreContext ctx, int impact) implements ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return ctx.inliner;
        }

        @Override
        public void undo() {
            ctx.inliner.softScores[ctx.scoreLevel] -= impact;
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofSoft(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardImpact(BendableScoreContext ctx, int impact) implements ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return ctx.inliner;
        }

        @Override
        public void undo() {
            ctx.inliner.hardScores[ctx.scoreLevel] -= impact;
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofHard(ctx.hardScoreLevelCount, ctx.softScoreLevelCount, ctx.scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexImpact(BendableScoreContext ctx, int[] hardImpacts,
            int[] softImpacts) implements ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return ctx.inliner;
        }

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
        public BendableScore toScore() {
            return BendableScore.of(hardImpacts, softImpacts);
        }
    }

}
