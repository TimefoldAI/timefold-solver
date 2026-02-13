package ai.timefold.solver.core.impl.score.definition;

import java.util.Arrays;
import java.util.stream.LongStream;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class BendableScoreDefinition extends AbstractBendableScoreDefinition<BendableScore> {

    public BendableScoreDefinition(int hardLevelsSize, int softLevelsSize) {
        super(hardLevelsSize, softLevelsSize);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Class<BendableScore> getScoreClass() {
        return BendableScore.class;
    }

    @Override
    public BendableScore getZeroScore() {
        return BendableScore.zero(hardLevelsSize, softLevelsSize);
    }

    @Override
    public final BendableScore getOneSoftestScore() {
        return BendableScore.ofSoft(hardLevelsSize, softLevelsSize, softLevelsSize - 1, 1L);
    }

    @Override
    public BendableScore parseScore(String scoreString) {
        var score = BendableScore.parseScore(scoreString);
        if (score.hardLevelsSize() != hardLevelsSize) {
            throw new IllegalArgumentException("The scoreString (" + scoreString
                    + ") for the scoreClass (" + BendableScore.class.getSimpleName()
                    + ") doesn't follow the correct pattern:"
                    + " the hardLevelsSize (" + score.hardLevelsSize()
                    + ") doesn't match the scoreDefinition's hardLevelsSize (" + hardLevelsSize + ").");
        }
        if (score.softLevelsSize() != softLevelsSize) {
            throw new IllegalArgumentException("The scoreString (" + scoreString
                    + ") for the scoreClass (" + BendableScore.class.getSimpleName()
                    + ") doesn't follow the correct pattern:"
                    + " the softLevelsSize (" + score.softLevelsSize()
                    + ") doesn't match the scoreDefinition's softLevelsSize (" + softLevelsSize + ").");
        }
        return score;
    }

    @Override
    public BendableScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        var hardScores = new long[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = (Long) levelNumbers[i];
        }
        var softScores = new long[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = (Long) levelNumbers[hardLevelsSize + i];
        }
        return BendableScore.of(hardScores, softScores);
    }

    public BendableScore createScore(long... scores) {
        var levelsSize = hardLevelsSize + softLevelsSize;
        if (scores.length != levelsSize) {
            throw new IllegalArgumentException("The scores (" + Arrays.toString(scores)
                    + ")'s length (" + scores.length
                    + ") is not levelsSize (" + levelsSize + ").");
        }
        return BendableScore.of(Arrays.copyOfRange(scores, 0, hardLevelsSize),
                Arrays.copyOfRange(scores, hardLevelsSize, levelsSize));
    }

    @Override
    public BendableScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            BendableScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        var hardScores = new long[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = (trendLevels[i] == InitializingScoreTrendLevel.ONLY_DOWN)
                    ? score.hardScore(i)
                    : Long.MAX_VALUE;
        }
        var softScores = new long[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = (trendLevels[hardLevelsSize + i] == InitializingScoreTrendLevel.ONLY_DOWN)
                    ? score.softScore(i)
                    : Long.MAX_VALUE;
        }
        return BendableScore.of(hardScores, softScores);
    }

    @Override
    public BendableScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            BendableScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        var hardScores = new long[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = (trendLevels[i] == InitializingScoreTrendLevel.ONLY_UP)
                    ? score.hardScore(i)
                    : Long.MIN_VALUE;
        }
        var softScores = new long[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = (trendLevels[hardLevelsSize + i] == InitializingScoreTrendLevel.ONLY_UP)
                    ? score.softScore(i)
                    : Long.MIN_VALUE;
        }
        return BendableScore.of(hardScores, softScores);
    }

    @Override
    public BendableScore divideBySanitizedDivisor(BendableScore dividend, BendableScore divisor) {
        var hardScores = new long[hardLevelsSize];
        for (var i = 0; i < hardLevelsSize; i++) {
            hardScores[i] = divide(dividend.hardScore(i), sanitize(divisor.hardScore(i)));
        }
        var softScores = new long[softLevelsSize];
        for (var i = 0; i < softLevelsSize; i++) {
            softScores[i] = divide(dividend.softScore(i), sanitize(divisor.softScore(i)));
        }
        var levels = LongStream.concat(Arrays.stream(hardScores), Arrays.stream(softScores)).toArray();
        return createScore(levels);
    }

    @Override
    public Class<?> getNumericType() {
        return long.class;
    }
}
