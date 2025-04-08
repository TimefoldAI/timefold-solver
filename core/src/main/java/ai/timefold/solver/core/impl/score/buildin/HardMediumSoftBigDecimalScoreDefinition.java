package ai.timefold.solver.core.impl.score.buildin;

import java.math.BigDecimal;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardMediumSoftBigDecimalScoreDefinition extends AbstractScoreDefinition<HardMediumSoftBigDecimalScore> {

    public HardMediumSoftBigDecimalScoreDefinition() {
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
    public Class<HardMediumSoftBigDecimalScore> getScoreClass() {
        return HardMediumSoftBigDecimalScore.class;
    }

    @Override
    public HardMediumSoftBigDecimalScore getZeroScore() {
        return HardMediumSoftBigDecimalScore.ZERO;
    }

    @Override
    public HardMediumSoftBigDecimalScore getOneSoftestScore() {
        return HardMediumSoftBigDecimalScore.ONE_SOFT;
    }

    @Override
    public HardMediumSoftBigDecimalScore parseScore(String scoreString) {
        return HardMediumSoftBigDecimalScore.parseScore(scoreString);
    }

    @Override
    public HardMediumSoftBigDecimalScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardMediumSoftBigDecimalScore.of((BigDecimal) levelNumbers[0], (BigDecimal) levelNumbers[1],
                (BigDecimal) levelNumbers[2]);
    }

    @Override
    public HardMediumSoftBigDecimalScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public HardMediumSoftBigDecimalScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public HardMediumSoftBigDecimalScore divideBySanitizedDivisor(HardMediumSoftBigDecimalScore dividend,
            HardMediumSoftBigDecimalScore divisor) {
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
        return BigDecimal.class;
    }
}
