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
    public SimpleLongScore fromLevelNumbers(Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return SimpleLongScore.of((Long) levelNumbers[0]);
    }

    @Override
    public SimpleLongScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend, SimpleLongScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return SimpleLongScore.of(trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.score() : Long.MAX_VALUE);
    }

    @Override
    public SimpleLongScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend, SimpleLongScore score) {
        var trendLevels = initializingScoreTrend.trendLevels();
        return SimpleLongScore.of(trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.score() : Long.MIN_VALUE);
    }

    @Override
    public SimpleLongScore divideBySanitizedDivisor(SimpleLongScore dividend, SimpleLongScore divisor) {
        var dividendScore = dividend.score();
        var divisorScore = sanitize(divisor.score());
        return fromLevelNumbers(
                new Number[] {
                        divide(dividendScore, divisorScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return long.class;
    }
}
