package ai.timefold.solver.core.impl.score.buildin;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardSoftScoreDefinition extends AbstractScoreDefinition<HardSoftScore> {

    public HardSoftScoreDefinition() {
        super(new String[] { "hard score", "soft score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getFeasibleLevelsSize() {
        return 1;
    }

    @Override
    public Class<HardSoftScore> getScoreClass() {
        return HardSoftScore.class;
    }

    @Override
    public HardSoftScore getZeroScore() {
        return HardSoftScore.ZERO;
    }

    @Override
    public HardSoftScore getOneSoftestScore() {
        return HardSoftScore.ONE_SOFT;
    }

    @Override
    public HardSoftScore parseScore(String scoreString) {
        return HardSoftScore.parseScore(scoreString);
    }

    @Override
    public HardSoftScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardSoftScore.of((Integer) levelNumbers[0], (Integer) levelNumbers[1]);
    }

    @Override
    public HardSoftScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend, HardSoftScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardSoftScore.of(trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.hardScore() : Integer.MAX_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_DOWN ? score.softScore() : Integer.MAX_VALUE);
    }

    @Override
    public HardSoftScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend, HardSoftScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return HardSoftScore.of(trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.hardScore() : Integer.MIN_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_UP ? score.softScore() : Integer.MIN_VALUE);
    }

    @Override
    public HardSoftScore divideBySanitizedDivisor(HardSoftScore dividend, HardSoftScore divisor) {
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
        return int.class;
    }
}
