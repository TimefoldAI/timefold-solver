package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class HardMediumSoftBigDecimalScoreContext
        extends ScoreContext<HardMediumSoftBigDecimalScore, HardMediumSoftBigDecimalScoreInliner> {

    public HardMediumSoftBigDecimalScoreContext(HardMediumSoftBigDecimalScoreInliner parent,
            Constraint constraint, HardMediumSoftBigDecimalScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore = parent.softScore.subtract(softImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofSoft(softImpact),
                justificationsSupplier);
    }

    public UndoScoreImpacter changeMediumScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal mediumImpact = constraintWeight.mediumScore().multiply(matchWeight);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.mediumScore = parent.mediumScore.subtract(mediumImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofMedium(mediumImpact),
                justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore = parent.hardScore.subtract(hardImpact);
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftBigDecimalScore.ofHard(hardImpact),
                justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        BigDecimal mediumImpact = constraintWeight.mediumScore().multiply(matchWeight);
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
        parent.hardScore = parent.hardScore.add(hardImpact);
        parent.mediumScore = parent.mediumScore.add(mediumImpact);
        parent.softScore = parent.softScore.add(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore = parent.hardScore.subtract(hardImpact);
            parent.mediumScore = parent.mediumScore.subtract(mediumImpact);
            parent.softScore = parent.softScore.subtract(softImpact);
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact,
                HardMediumSoftBigDecimalScore.of(hardImpact, mediumImpact, softImpact), justificationsSupplier);
    }

}
