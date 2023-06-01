package ai.timefold.solver.constraint.streams.common.inliner;

import java.math.BigDecimal;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

final class HardSoftBigDecimalScoreContext extends ScoreContext<HardSoftBigDecimalScore> {

    private final Consumer<BigDecimal> softScoreUpdater;
    private final Consumer<BigDecimal> hardScoreUpdater;

    public HardSoftBigDecimalScoreContext(AbstractScoreInliner<HardSoftBigDecimalScore> parent, Constraint constraint,
            HardSoftBigDecimalScore constraintWeight, Consumer<BigDecimal> hardScoreUpdater,
            Consumer<BigDecimal> softScoreUpdater) {
        super(parent, constraint, constraintWeight);
        this.softScoreUpdater = softScoreUpdater;
        this.hardScoreUpdater = hardScoreUpdater;
    }

    public UndoScoreImpacter changeSoftScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
        softScoreUpdater.accept(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> softScoreUpdater.accept(softImpact.negate());
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofSoft(softImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeHardScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        hardScoreUpdater.accept(hardImpact);
        UndoScoreImpacter undoScoreImpact = () -> hardScoreUpdater.accept(hardImpact.negate());
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.ofHard(hardImpact), justificationsSupplier);
    }

    public UndoScoreImpacter changeScoreBy(BigDecimal matchWeight, JustificationsSupplier justificationsSupplier) {
        BigDecimal hardImpact = constraintWeight.hardScore().multiply(matchWeight);
        BigDecimal softImpact = constraintWeight.softScore().multiply(matchWeight);
        hardScoreUpdater.accept(hardImpact);
        softScoreUpdater.accept(softImpact);
        UndoScoreImpacter undoScoreImpact = () -> {
            hardScoreUpdater.accept(hardImpact.negate());
            softScoreUpdater.accept(softImpact.negate());
        };
        if (!constraintMatchEnabled) {
            return undoScoreImpact;
        }
        return impactWithConstraintMatch(undoScoreImpact, HardSoftBigDecimalScore.of(hardImpact, softImpact),
                justificationsSupplier);
    }

}
