package ai.timefold.solver.core.impl.score.buildin;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardSoftLongScoreDefinition extends AbstractScoreDefinition<HardSoftLongScore> {

    public HardSoftLongScoreDefinition() {
        super(new String[] { "hard score", "soft score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getLevelsSize() {
        return 2;
    }

    @Override
    public int getFeasibleLevelsSize() {
        return 1;
    }

    @Override
    public Class<HardSoftLongScore> getScoreClass() {
        return HardSoftLongScore.class;
    }

    @Override
    public HardSoftLongScore getZeroScore() {
        return HardSoftLongScore.ZERO;
    }

    @Override
    public HardSoftLongScore getOneSoftestScore() {
        return HardSoftLongScore.ONE_SOFT;
    }

    @Override
    public HardSoftLongScore parseScore(String scoreString) {
        return HardSoftLongScore.parseScore(scoreString);
    }

    @Override
    public HardSoftLongScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardSoftLongScore.of((Long) levelNumbers[0], (Long) levelNumbers[1]);
    }

    @Override
    public HardSoftLongScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardSoftLongScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardSoftLongScore.of(
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.hardScore() : Long.MAX_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_DOWN ? score.softScore() : Long.MAX_VALUE);
    }

    @Override
    public HardSoftLongScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardSoftLongScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardSoftLongScore.of(trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.hardScore() : Long.MIN_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_UP ? score.softScore() : Long.MIN_VALUE);
    }

    @Override
    public HardSoftLongScore divideBySanitizedDivisor(HardSoftLongScore dividend, HardSoftLongScore divisor) {
        var dividendHardScore = dividend.hardScore();
        var divisorHardScore = sanitize(divisor.hardScore());
        var dividendSoftScore = dividend.softScore();
        var divisorSoftScore = sanitize(divisor.softScore());
        return fromLevelNumbers(
                new Number[] {
                        divide(dividendHardScore, divisorHardScore),
                        divide(dividendSoftScore, divisorSoftScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return long.class;
    }
}
