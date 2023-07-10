package ai.timefold.solver.constraint.streams.common.inliner;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;

public final class BendableLongScoreInliner extends AbstractScoreInliner<BendableLongScore> {

    final long[] hardScores;
    final long[] softScores;

    BendableLongScoreInliner(boolean constraintMatchEnabled, int hardLevelsSize, int softLevelsSize) {
        super(constraintMatchEnabled);
        hardScores = new long[hardLevelsSize];
        softScores = new long[softLevelsSize];
    }

    @Override
    public WeightedScoreImpacter<BendableLongScore, BendableLongScoreContext> buildWeightedScoreImpacter(Constraint constraint,
            BendableLongScore constraintWeight) {
        validateConstraintWeight(constraint, constraintWeight);
        Integer singleLevel = null;
        for (int i = 0; i < constraintWeight.levelsSize(); i++) {
            if (constraintWeight.hardOrSoftScore(i) != 0L) {
                if (singleLevel != null) {
                    singleLevel = null;
                    break;
                }
                singleLevel = i;
            }
        }
        if (singleLevel != null) {
            boolean isHardScore = singleLevel < constraintWeight.hardLevelsSize();
            int level = isHardScore ? singleLevel : singleLevel - constraintWeight.hardLevelsSize();
            BendableLongScoreContext context = new BendableLongScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length, level, constraintWeight.hardOrSoftScore(singleLevel));
            if (isHardScore) {
                return WeightedScoreImpacter.of(context, (BendableLongScoreContext ctx, long impact,
                        JustificationsSupplier justificationSupplier) -> ctx.changeHardScoreBy(impact, justificationSupplier));
            } else {
                return WeightedScoreImpacter.of(context, (BendableLongScoreContext ctx, long impact,
                        JustificationsSupplier justificationSupplier) -> ctx.changeSoftScoreBy(impact, justificationSupplier));
            }
        } else {
            BendableLongScoreContext context =
                    new BendableLongScoreContext(this, constraint, constraintWeight, hardScores.length, softScores.length);
            return WeightedScoreImpacter.of(context, (BendableLongScoreContext ctx, long impact,
                    JustificationsSupplier justificationSupplier) -> ctx.changeScoreBy(impact, justificationSupplier));
        }
    }

    @Override
    public BendableLongScore extractScore(int initScore) {
        return BendableLongScore.ofUninitialized(initScore,
                Arrays.copyOf(hardScores, hardScores.length),
                Arrays.copyOf(softScores, softScores.length));
    }

    @Override
    public String toString() {
        return BendableLongScore.class.getSimpleName() + " inliner";
    }

}
