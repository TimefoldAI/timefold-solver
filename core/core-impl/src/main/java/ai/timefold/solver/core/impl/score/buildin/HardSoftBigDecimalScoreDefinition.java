package ai.timefold.solver.core.impl.score.buildin;

import java.math.BigDecimal;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardSoftBigDecimalScoreDefinition extends AbstractScoreDefinition<HardSoftBigDecimalScore> {

    public HardSoftBigDecimalScoreDefinition() {
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
    public Class<HardSoftBigDecimalScore> getScoreClass() {
        return HardSoftBigDecimalScore.class;
    }

    @Override
    public HardSoftBigDecimalScore getZeroScore() {
        return HardSoftBigDecimalScore.ZERO;
    }

    @Override
    public HardSoftBigDecimalScore getOneSoftestScore() {
        return HardSoftBigDecimalScore.ONE_SOFT;
    }

    @Override
    public HardSoftBigDecimalScore parseScore(String scoreString) {
        return HardSoftBigDecimalScore.parseScore(scoreString);
    }

    @Override
    public HardSoftBigDecimalScore fromLevelNumbers(int initScore, Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardSoftBigDecimalScore.ofUninitialized(initScore, (BigDecimal) levelNumbers[0], (BigDecimal) levelNumbers[1]);
    }

    @Override
    public HardSoftBigDecimalScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardSoftBigDecimalScore score) {
        // TODO https://issues.redhat.com/browse/PLANNER-232
        throw new UnsupportedOperationException("PLANNER-232: BigDecimalScore does not support bounds" +
                " because a BigDecimal cannot represent infinity.");
    }

    @Override
    public HardSoftBigDecimalScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardSoftBigDecimalScore score) {
        // TODO https://issues.redhat.com/browse/PLANNER-232
        throw new UnsupportedOperationException("PLANNER-232: BigDecimalScore does not support bounds" +
                " because a BigDecimal cannot represent infinity.");
    }

    @Override
    public HardSoftBigDecimalScore divideBySanitizedDivisor(HardSoftBigDecimalScore dividend,
            HardSoftBigDecimalScore divisor) {
        int dividendInitScore = dividend.initScore();
        int divisorInitScore = sanitize(divisor.initScore());
        BigDecimal dividendHardScore = dividend.hardScore();
        BigDecimal divisorHardScore = sanitize(divisor.hardScore());
        BigDecimal dividendSoftScore = dividend.softScore();
        BigDecimal divisorSoftScore = sanitize(divisor.softScore());
        return fromLevelNumbers(
                divide(dividendInitScore, divisorInitScore),
                new Number[] {
                        divide(dividendHardScore, divisorHardScore),
                        divide(dividendSoftScore, divisorSoftScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return BigDecimal.class;
    }
}
