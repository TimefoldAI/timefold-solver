package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardMediumSoftLongScoreContext extends ScoreContext<HardMediumSoftLongScore, HardMediumSoftLongScoreInliner> {

    public HardMediumSoftLongScoreContext(HardMediumSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardMediumSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        long softImpact = constraintWeight.softScore() * matchWeight;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofSoft(softImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeMediumScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        long mediumImpact = constraintWeight.mediumScore() * matchWeight;
        parent.mediumScore += mediumImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.mediumScore -= mediumImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofMedium(mediumImpact),
                constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
        long hardImpact = constraintWeight.hardScore() * matchWeight;
        parent.hardScore += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardMediumSoftLongScore.ofHard(hardImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardMediumSoftLongScore> constraintMatchSupplier) {
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
                constraintMatchSupplier);
    }

}
