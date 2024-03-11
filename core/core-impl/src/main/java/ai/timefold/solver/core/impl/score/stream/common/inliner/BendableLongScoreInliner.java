package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

public final class BendableLongScoreInliner extends AbstractScoreInliner<BendableLongScore> {

    final long[] hardScores;
    final long[] softScores;

    BendableLongScoreInliner(Map<Constraint, BendableLongScore> constraintWeightMap, boolean constraintMatchEnabled,
            int hardLevelsSize, int softLevelsSize) {
        super(constraintWeightMap, constraintMatchEnabled);
        hardScores = new long[hardLevelsSize];
        softScores = new long[softLevelsSize];
    }

    @Override
    public WeightedScoreImpacter<BendableLongScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        Integer singleLevel = null;
        BendableLongScore constraintWeight = constraintWeightMap.get(constraint);
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
                        ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) -> ctx.changeHardScoreBy(impact,
                                constraintMatchSupplier));
            } else {
                return WeightedScoreImpacter.of(context, (BendableLongScoreContext ctx, long impact,
                        ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) -> ctx.changeSoftScoreBy(impact,
                                constraintMatchSupplier));
            }
        } else {
            BendableLongScoreContext context =
                    new BendableLongScoreContext(this, constraint, constraintWeight, hardScores.length, softScores.length);
            return WeightedScoreImpacter.of(context, (BendableLongScoreContext ctx, long impact,
                    ConstraintMatchSupplier<BendableLongScore> constraintMatchSupplier) -> ctx.changeScoreBy(impact,
                            constraintMatchSupplier));
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
