package ai.timefold.solver.core.impl.score.buildin;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.core.impl.score.definition.AbstractBendableScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class BendableBigDecimalScoreDefinition extends AbstractBendableScoreDefinition<BendableBigDecimalScore> {

    public BendableBigDecimalScoreDefinition(int hardLevelsSize, int softLevelsSize) {
        super(hardLevelsSize, softLevelsSize);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Class<BendableBigDecimalScore> getScoreClass() {
        return BendableBigDecimalScore.class;
    }

    @Override
    public BendableBigDecimalScore getZeroScore() {
        return BendableBigDecimalScore.zero(hardLevelsSize, softLevelsSize);
    }

    @Override
    public final BendableBigDecimalScore getOneSoftestScore() {
        return BendableBigDecimalScore.ofSoft(hardLevelsSize, softLevelsSize, softLevelsSize - 1, BigDecimal.ONE);
    }

    @Override
    public BendableBigDecimalScore parseScore(String scoreString) {
        var score = BendableBigDecimalScore.parseScore(scoreString);
        if (score.hardLevelsSize() != hardLevelsSize) {
            throw new IllegalArgumentException("The scoreString (" + scoreString
                    + ") for the scoreClass (" + BendableBigDecimalScore.class.getSimpleName()
                    + ") doesn't follow the correct pattern:"
                    + " the hardLevelsSize (" + score.hardLevelsSize()
                    + ") doesn't match the scoreDefinition's hardLevelsSize (" + hardLevelsSize + ").");
        }
        if (score.softLevelsSize() != softLevelsSize) {
            throw new IllegalArgumentException("The scoreString (" + scoreString
                    + ") for the scoreClass (" + BendableBigDecimalScore.class.getSimpleName()
                    + ") doesn't follow the correct pattern:"
                    + " the softLevelsSize (" + score.softLevelsSize()
                    + ") doesn't match the scoreDefinition's softLevelsSize (" + softLevelsSize + ").");
        }
        return score;
    }

    @Override
    public BendableBigDecimalScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        var hardScores = new BigDecimal[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = (BigDecimal) levelNumbers[i];
        }
        var softScores = new BigDecimal[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = (BigDecimal) levelNumbers[hardLevelsSize + i];
        }
        return BendableBigDecimalScore.of(hardScores, softScores);
    }

    public BendableBigDecimalScore createScore(BigDecimal... scores) {
        var levelsSize = hardLevelsSize + softLevelsSize;
        if (scores.length != levelsSize) {
            throw new IllegalArgumentException("The scores (" + Arrays.toString(scores)
                    + ")'s length (" + scores.length
                    + ") is not levelsSize (" + levelsSize + ").");
        }
        return BendableBigDecimalScore.of(Arrays.copyOfRange(scores, 0, hardLevelsSize),
                Arrays.copyOfRange(scores, hardLevelsSize, levelsSize));
    }

    @Override
    public BendableBigDecimalScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            BendableBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public BendableBigDecimalScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            BendableBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public BendableBigDecimalScore divideBySanitizedDivisor(BendableBigDecimalScore dividend,
            BendableBigDecimalScore divisor) {
        var hardScores = new BigDecimal[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = divide(dividend.hardScore(i), sanitize(divisor.hardScore(i)));
        }
        var softScores = new BigDecimal[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = divide(dividend.softScore(i), sanitize(divisor.softScore(i)));
        }
        var levels = Stream.concat(Arrays.stream(hardScores), Arrays.stream(softScores))
                .toArray(BigDecimal[]::new);
        return createScore(levels);
    }

    @Override
    public Class<?> getNumericType() {
        return BigDecimal.class;
    }
}
