package ai.timefold.solver.core.impl.score.buildin;

import java.math.BigDecimal;
import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class SimpleBigDecimalScoreDefinition extends AbstractScoreDefinition<SimpleBigDecimalScore> {

    public SimpleBigDecimalScoreDefinition() {
        super(new String[] { "score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getLevelsSize() {
        return 1;
    }

    @Override
    public int getFeasibleLevelsSize() {
        return 0;
    }

    @Override
    public Class<SimpleBigDecimalScore> getScoreClass() {
        return SimpleBigDecimalScore.class;
    }

    @Override
    public SimpleBigDecimalScore getZeroScore() {
        return SimpleBigDecimalScore.ZERO;
    }

    @Override
    public SimpleBigDecimalScore getOneSoftestScore() {
        return SimpleBigDecimalScore.ONE;
    }

    @Override
    public SimpleBigDecimalScore parseScore(String scoreString) {
        return SimpleBigDecimalScore.parseScore(scoreString);
    }

    @Override
    public SimpleBigDecimalScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return SimpleBigDecimalScore.of((BigDecimal) levelNumbers[0]);
    }

    @Override
    public SimpleBigDecimalScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            SimpleBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public SimpleBigDecimalScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            SimpleBigDecimalScore score) {
        throw new UnsupportedOperationException(
                "BigDecimalScore does not support bounds because a BigDecimal cannot represent infinity.");
    }

    @Override
    public SimpleBigDecimalScore divideBySanitizedDivisor(SimpleBigDecimalScore dividend,
            SimpleBigDecimalScore divisor) {
        var dividendScore = dividend.score();
        var divisorScore = sanitize(divisor.score());
        return fromLevelNumbers(
                new Number[] {
                        divide(dividendScore, divisorScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return BigDecimal.class;
    }
}
