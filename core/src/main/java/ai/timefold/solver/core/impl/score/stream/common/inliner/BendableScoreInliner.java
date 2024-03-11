package ai.timefold.solver.core.impl.score.stream.common.inliner;

import java.util.Arrays;
import java.util.Map;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraint;

final class BendableScoreInliner extends AbstractScoreInliner<BendableScore> {

    final int[] hardScores;
    final int[] softScores;

    BendableScoreInliner(Map<Constraint, BendableScore> constraintWeightMap, boolean constraintMatchEnabled, int hardLevelsSize,
            int softLevelsSize) {
        super(constraintWeightMap, constraintMatchEnabled);
        hardScores = new int[hardLevelsSize];
        softScores = new int[softLevelsSize];
    }

    @Override
    public WeightedScoreImpacter<BendableScore, ?> buildWeightedScoreImpacter(AbstractConstraint<?, ?, ?> constraint) {
        Integer singleLevel = null;
        BendableScore constraintWeight = constraintWeightMap.get(constraint);
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
            BendableScoreContext context = new BendableScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length, level, constraintWeight.hardOrSoftScore(singleLevel));
            if (isHardScore) {
                return WeightedScoreImpacter.of(context, BendableScoreContext::changeHardScoreBy);
            } else {
                return WeightedScoreImpacter.of(context, BendableScoreContext::changeSoftScoreBy);
            }
        } else {
            BendableScoreContext context = new BendableScoreContext(this, constraint, constraintWeight,
                    hardScores.length, softScores.length);
            return WeightedScoreImpacter.of(context, BendableScoreContext::changeScoreBy);
        }
    }

    @Override
    public BendableScore extractScore(int initScore) {
        return BendableScore.ofUninitialized(initScore,
                Arrays.copyOf(hardScores, hardScores.length),
                Arrays.copyOf(softScores, softScores.length));
    }

    @Override
    public String toString() {
        return BendableScore.class.getSimpleName() + " inliner";
    }

}
