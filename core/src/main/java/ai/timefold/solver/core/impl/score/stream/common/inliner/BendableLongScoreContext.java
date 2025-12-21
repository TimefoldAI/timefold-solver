package ai.timefold.solver.core.impl.score.stream.common.inliner;

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
        parent.softScores[scoreLevel] += softImpact;
        var scoreImpact =
                new SingleSoftLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableLongScore> changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        var hardImpact = scoreLevelWeight * matchWeight;
        parent.hardScores[scoreLevel] += hardImpact;
        var scoreImpact =
                new SingleHardLevelScoreImpact(parent, hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact);
        if (!constraintMatchPolicy.isEnabled()) {
            return scoreImpact;
        }
        return impactWithConstraintMatch(scoreImpact, constraintMatchSupplier);
    }

    public ScoreImpact<BendableLongScore> changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        var hardImpacts = new long[hardScoreLevelCount];
        var softImpacts = new long[softScoreLevelCount];
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
    private record SingleSoftLevelScoreImpact(BendableLongScoreInliner inliner, int hardScoreLevelCount,
            int softScoreLevelCount, int scoreLevel, long impact)
            implements
                ScoreImpact<BendableLongScore> {

        @Override
        public AbstractScoreInliner<BendableLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.softScores[scoreLevel] -= impact;
        }

        @Override
        public BendableLongScore toScore() {
            return BendableLongScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record SingleHardLevelScoreImpact(BendableLongScoreInliner inliner, int hardScoreLevelCount,
            int softScoreLevelCount, int scoreLevel, long impact)
            implements
                ScoreImpact<BendableLongScore> {

        @Override
        public AbstractScoreInliner<BendableLongScore> scoreInliner() {
            return inliner;
        }

        @Override
        public void undo() {
            inliner.hardScores[scoreLevel] -= impact;
        }

        @Override
        public BendableLongScore toScore() {
            return BendableLongScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, impact);
        }
    }

    @NullMarked
    private record ComplexScoreImpact(BendableLongScoreInliner inliner, int hardScoreLevelCount, int softScoreLevelCount,
            long[] hardImpacts, long[] softImpacts)
            implements
                ScoreImpact<BendableLongScore> {

        @Override
        public AbstractScoreInliner<BendableLongScore> scoreInliner() {
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
        public BendableLongScore toScore() {
            return BendableLongScore.of(hardImpacts, softImpacts);
        }
    }

}
