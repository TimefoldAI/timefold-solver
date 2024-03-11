package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

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

    public UndoScoreImpacter changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        long softImpact = scoreLevelWeight * matchWeight;
        parent.softScores[scoreLevel] += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScores[scoreLevel] -= softImpact;
        ;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableLongScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        long hardImpact = scoreLevelWeight * matchWeight;
        parent.hardScores[scoreLevel] += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScores[scoreLevel] -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableLongScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) {
        long[] hardImpacts = new long[hardScoreLevelCount];
        long[] softImpacts = new long[softScoreLevelCount];
        for (int hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            long hardImpact = constraintWeight.hardScore(hardScoreLevel) * matchWeight;
            hardImpacts[hardScoreLevel] = hardImpact;
            parent.hardScores[hardScoreLevel] += hardImpact;
        }
        for (int softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            long softImpact = constraintWeight.softScore(softScoreLevel) * matchWeight;
            softImpacts[softScoreLevel] = softImpact;
            parent.softScores[softScoreLevel] += softImpact;
        }
        UndoScoreImpacter undoScoreImpact = () -> {
            for (int hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
                parent.hardScores[hardScoreLevel] -= hardImpacts[hardScoreLevel];
            }
            for (int softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
                parent.softScores[softScoreLevel] -= softImpacts[softScoreLevel];
            }
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, BendableLongScore.of(hardImpacts, softImpacts),
                constraintMatchSupplier);
    }

}
