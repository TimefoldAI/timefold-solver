package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;

final class HardMediumSoftLongScoreContext extends ScoreContext<HardMediumSoftLongScore, HardMediumSoftLongScoreInliner> {

    public HardMediumSoftLongScoreContext(HardMediumSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(long matchWeight, JustificationsSupplier justificationsSupplier) {
        long softImpact = constraintWeight.softScore() * matchWeight;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofSoft(softImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeMediumScoreBy(long matchWeight, JustificationsSupplier justificationsSupplier) {
        long mediumImpact = constraintWeight.mediumScore() * matchWeight;
        parent.mediumScore += mediumImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.mediumScore -= mediumImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofMedium(mediumImpact),
                justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(long matchWeight, JustificationsSupplier justificationsSupplier) {
        long hardImpact = constraintWeight.hardScore() * matchWeight;
        parent.hardScore += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofHard(hardImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight, JustificationsSupplier justificationsSupplier) {
        long hardImpact = constraintWeight.hardScore() * matchWeight;
        long mediumImpact = constraintWeight.mediumScore() * matchWeight;
        long softImpact = constraintWeight.softScore() * matchWeight;
        parent.hardScore += hardImpact;
        parent.mediumScore += mediumImpact;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore -= hardImpact;
            parent.mediumScore -= mediumImpact;
            parent.softScore -= softImpact;
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.of(hardImpact, mediumImpact, softImpact),
                justificationsSupplier);
    }

}
