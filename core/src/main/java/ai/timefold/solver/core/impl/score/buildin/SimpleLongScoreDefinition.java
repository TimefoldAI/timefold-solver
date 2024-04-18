package ai.timefold.solver.core.impl.score.buildin;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class SimpleLongScoreDefinition extends AbstractScoreDefinition<SimpleLongScore> {

    public SimpleLongScoreDefinition() {
        super(new String[] { "score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getFeasibleLevelsSize() {
        return 0;
    }

    @Override
    public Class<SimpleLongScore> getScoreClass() {
        return SimpleLongScore.class;
    }

    @Override
    public SimpleLongScore getZeroScore() {
        return SimpleLongScore.ZERO;
    }

    @Override
    public SimpleLongScore getOneSoftestScore() {
        return SimpleLongScore.ONE;
    }

    @Override
    public SimpleLongScore parseScore(String scoreString) {
        return SimpleLongScore.parseScore(scoreString);
    }

    @Override
    public SimpleLongScore fromLevelNumbers(int initScore, Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return SimpleLongScore.ofUninitialized(initScore, (Long) levelNumbers[0]);
    }

    @Override
    public SimpleLongScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend, SimpleLongScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.trendLevels();
        return SimpleLongScore.ofUninitialized(0,
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.score() : Long.MAX_VALUE);
    }

    @Override
    public SimpleLongScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend, SimpleLongScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.trendLevels();
        return SimpleLongScore.ofUninitialized(0,
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.score() : Long.MIN_VALUE);
    }

    @Override
    public SimpleLongScore divideBySanitizedDivisor(SimpleLongScore dividend, SimpleLongScore divisor) {
        int dividendInitScore = dividend.initScore();
        int divisorInitScore = sanitize(divisor.initScore());
        long dividendScore = dividend.score();
        long divisorScore = sanitize(divisor.score());
        return fromLevelNumbers(
                divide(dividendInitScore, divisorInitScore),
                new Number[] {
                        divide(dividendScore, divisorScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return long.class;
    }
}
