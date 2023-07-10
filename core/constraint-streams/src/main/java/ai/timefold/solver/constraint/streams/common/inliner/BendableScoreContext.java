package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class BendableScoreContext extends ScoreContext<BendableScore, BendableScoreInliner> {

    private final int hardScoreLevelCount;
    private final int softScoreLevelCount;
    private final int scoreLevel;
    private final int scoreLevelWeight;

    public BendableScoreContext(BendableScoreInliner parent, Constraint constraint, BendableScore constraintWeight,
            int hardScoreLevelCount, int softScoreLevelCount, int scoreLevel, int scoreLevelWeight) {
        super(parent, constraint, constraintWeight);
        this.hardScoreLevelCount = hardScoreLevelCount;
        this.softScoreLevelCount = softScoreLevelCount;
        this.scoreLevel = scoreLevel;
        this.scoreLevelWeight = scoreLevelWeight;
    }

    public BendableScoreContext(BendableScoreInliner parent, Constraint constraint, BendableScore constraintWeight,
            int hardScoreLevelCount, int softScoreLevelCount) {
        this(parent, constraint, constraintWeight, hardScoreLevelCount, softScoreLevelCount, -1, -1);
    }

    public UndoScoreImpacter changeSoftScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int softImpact = scoreLevelWeight * matchWeight;
        parent.softScores[scoreLevel] += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScores[scoreLevel] -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableScore.ofSoft(hardScoreLevelCount, softScoreLevelCount, scoreLevel, softImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int hardImpact = scoreLevelWeight * matchWeight;
        parent.hardScores[scoreLevel] += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScores[scoreLevel] -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                BendableScore.ofHard(hardScoreLevelCount, softScoreLevelCount, scoreLevel, hardImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int[] hardImpacts = new int[hardScoreLevelCount];
        int[] softImpacts = new int[softScoreLevelCount];
        for (int hardScoreLevel = 0; hardScoreLevel < hardScoreLevelCount; hardScoreLevel++) {
            int hardImpact = constraintWeight.hardScore(hardScoreLevel) * matchWeight;
            hardImpacts[hardScoreLevel] = hardImpact;
            parent.hardScores[hardScoreLevel] += hardImpact;
        }
        for (int softScoreLevel = 0; softScoreLevel < softScoreLevelCount; softScoreLevel++) {
            int softImpact = constraintWeight.softScore(softScoreLevel) * matchWeight;
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
        return impactWithConstraintMatch(undoScoreImpact, BendableScore.of(hardImpacts, softImpacts), justificationsSupplier);
    }

}
