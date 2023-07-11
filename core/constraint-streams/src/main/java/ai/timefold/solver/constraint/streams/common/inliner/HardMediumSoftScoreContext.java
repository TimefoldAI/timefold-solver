package ai.timefold.solver.constraint.streams.common.inliner;

import ai.timefold.solver.constraint.streams.common.AbstractConstraint;
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;

final class HardMediumSoftScoreContext extends ScoreContext<HardMediumSoftScore, HardMediumSoftScoreInliner> {

    public HardMediumSoftScoreContext(HardMediumSoftScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int softImpact = constraintWeight.softScore() * matchWeight;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftScore.ofSoft(softImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeMediumScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int mediumImpact = constraintWeight.mediumScore() * matchWeight;
        parent.mediumScore += mediumImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.mediumScore -= mediumImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftScore.ofMedium(mediumImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int hardImpact = constraintWeight.hardScore() * matchWeight;
        parent.hardScore += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftScore.ofHard(hardImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(int matchWeight, JustificationsSupplier justificationsSupplier) {
        int hardImpact = constraintWeight.hardScore() * matchWeight;
        int mediumImpact = constraintWeight.mediumScore() * matchWeight;
        int softImpact = constraintWeight.softScore() * matchWeight;
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
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftScore.of(hardImpact, mediumImpact, softImpact),
                justificationsSupplier);
    }

}
