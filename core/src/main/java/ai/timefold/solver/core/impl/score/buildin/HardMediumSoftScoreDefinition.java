package ai.timefold.solver.core.impl.score.buildin;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardMediumSoftScoreDefinition extends AbstractScoreDefinition<HardMediumSoftScore> {

    public HardMediumSoftScoreDefinition() {
        super(new String[] { "hard score", "medium score", "soft score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getLevelsSize() {
        return 3;
    }

    @Override
    public int getFeasibleLevelsSize() {
        return 1;
    }

    @Override
    public Class<HardMediumSoftScore> getScoreClass() {
        return HardMediumSoftScore.class;
    }

    @Override
    public HardMediumSoftScore getZeroScore() {
        return HardMediumSoftScore.ZERO;
    }

    @Override
    public HardMediumSoftScore getOneSoftestScore() {
        return HardMediumSoftScore.ONE_SOFT;
    }

    @Override
    public HardMediumSoftScore parseScore(String scoreString) {
        return HardMediumSoftScore.parseScore(scoreString);
    }

    @Override
    public HardMediumSoftScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardMediumSoftScore.of((Integer) levelNumbers[0], (Integer) levelNumbers[1], (Integer) levelNumbers[2]);
    }

    @Override
    public HardMediumSoftScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardMediumSoftScore.of(
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.hardScore() : Integer.MAX_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_DOWN ? score.mediumScore() : Integer.MAX_VALUE,
                trendLevels[2] == InitializingScoreTrendLevel.ONLY_DOWN ? score.softScore() : Integer.MAX_VALUE);
    }

    @Override
    public HardMediumSoftScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardMediumSoftScore.of(
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.hardScore() : Integer.MIN_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_UP ? score.mediumScore() : Integer.MIN_VALUE,
                trendLevels[2] == InitializingScoreTrendLevel.ONLY_UP ? score.softScore() : Integer.MIN_VALUE);
    }

    @Override
    public HardMediumSoftScore divideBySanitizedDivisor(HardMediumSoftScore dividend, HardMediumSoftScore divisor) {
        var dividendHardScore = dividend.hardScore();
        var divisorHardScore = sanitize(divisor.hardScore());
        var dividendMediumScore = dividend.mediumScore();
        var divisorMediumScore = sanitize(divisor.mediumScore());
        var dividendSoftScore = dividend.softScore();
        var divisorSoftScore = sanitize(divisor.softScore());
        return fromLevelNumbers(
                new Number[] {
                        divide(dividendHardScore, divisorHardScore),
                        divide(dividendMediumScore, divisorMediumScore),
                        divide(dividendSoftScore, divisorSoftScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return int.class;
    }
}
