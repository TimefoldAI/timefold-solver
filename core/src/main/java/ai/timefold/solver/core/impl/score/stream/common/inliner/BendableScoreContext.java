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
        parent.softScores[scoreLevel] += softImpact;
        var scoreImpact =
                new SingleSoftLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableScore> changeHardScoreBy(int matchWeight,
            ConstraintMatchSupplier<BendableScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight * matchWeight;
        parent.hardScores[scoreLevel] += hardImpact;
        var scoreImpact =
                new SingleHardLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact);
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
            parent.hardScores[hardScoreLevel] += hardImpact;
        }
        for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            var softImpact = constraintWeight.softScore(softScoreLevel) * matchWeight;
            softImpacts[softScoreLevel] = softImpact;
            parent.softScores[softScoreLevel] += softImpact;
        }
        var scoreImpact =
                new ComplexScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, hardImpacts, softImpacts);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    @NullMarked
    private record SingleSoftLevelScoreImpact(BendableScoreInliner inliner, int hardScoreLevelCount, int softScoreLevelCount,
            int scoreLevel, int impact)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScores[scoreLevel] -= impact;
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardLevelScoreImpact(BendableScoreInliner inliner, int hardScoreLevelCount, int softScoreLevelCount,
            int scoreLevel, int impact)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScores[scoreLevel] -= impact;
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexScoreImpact(BendableScoreInliner inliner, int hardScoreLevelCount, int softScoreLevelCount,
            int[] hardImpacts, int[] softImpacts)
            implements
                ScoreImpact<BendableScore> {

        @Override
        public AbstractScoreInliner<BendableScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            for (var hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
                inliner.hardScores[hardScoreLevel] -= hardImpacts[hardScoreLevel];
            }
            for (var softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
                inliner.softScores[softScoreLevel] -= softImpacts[softScoreLevel];
            }
        }

        @Override
        public BendableScore toScore() {
            return BendableScore.of(hardImpacts, softImpacts);
        }
    }

}
