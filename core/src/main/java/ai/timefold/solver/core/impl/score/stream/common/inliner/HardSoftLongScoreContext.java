package ai.timefold.solver.core.impl.score.stream.common.inliner;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class HardSoftLongScoreContext extends ScoreContext<HardSoftLongScore, HardSoftLongScoreInliner> {

    public HardSoftLongScoreContext(HardSoftLongScoreInliner parent, AbstractConstraint<?, ?, ?> constraint,
            HardSoftLongScore constraintWeight) {
        super(parent, constraint, constraintWeight);
    }

    public UndoScoreImpacter changeSoftScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
        long softImpact = constraintWeight.softScore() * matchWeight;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.softScore -= softImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftLongScore.ofSoft(softImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
        long hardImpact = constraintWeight.hardScore() * matchWeight;
        parent.hardScore += hardImpact;
        UndoScoreImpacter undoScoreImpact = () -> parent.hardScore -= hardImpact;
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftLongScore.ofHard(hardImpact), constraintMatchSupplier);
    }

    public UndoScoreImpacter changeScoreBy(long matchWeight,
            ConstraintMatchSupplier<HardSoftLongScore> constraintMatchSupplier) {
        long hardImpact = constraintWeight.hardScore() * matchWeight;
        long softImpact = constraintWeight.softScore() * matchWeight;
        parent.hardScore += hardImpact;
        parent.softScore += softImpact;
        UndoScoreImpacter undoScoreImpact = () -> {
            parent.hardScore -= hardImpact;
            parent.softScore -= softImpact;
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftLongScore.of(hardImpact, softImpact),
                constraintMatchSupplier);
    }

}
