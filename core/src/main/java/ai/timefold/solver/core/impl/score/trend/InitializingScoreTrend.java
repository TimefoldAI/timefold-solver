package ai.timefold.solver.core.impl.score.trend;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;

/**
 * Bounds the possible {@link Score}s for a {@link PlanningSolution} as more and more variables are initialized
 * (while the already initialized variables don't change).
 *
 * @see InitializingScoreTrendLevel
 */
public record InitializingScoreTrend(InitializingScoreTrendLevel[] trendLevels) {

    public static InitializingScoreTrend parseTrend(String initializingScoreTrendString, int levelsSize) {
        var trendTokens = initializingScoreTrendString.split("/");
        var tokenIsSingle = trendTokens.length == 1;
        if (!tokenIsSingle && trendTokens.length != levelsSize) {
            throw new IllegalArgumentException("""
                    The initializingScoreTrendString (%s) doesn't follow the correct pattern (%s): \
                    the trendTokens length (%d) differs from the levelsSize (%d)."""
                    .formatted(initializingScoreTrendString, buildTrendPattern(levelsSize), trendTokens.length,
                            levelsSize));
        }
        var trendLevels = new InitializingScoreTrendLevel[levelsSize];
        for (var i = 0; i < levelsSize; i++) {
            trendLevels[i] = InitializingScoreTrendLevel.valueOf(trendTokens[tokenIsSingle ? 0 : i]);
        }
        return new InitializingScoreTrend(trendLevels);
    }

    public static InitializingScoreTrend buildUniformTrend(InitializingScoreTrendLevel trendLevel, int levelsSize) {
        var trendLevels = new InitializingScoreTrendLevel[levelsSize];
        Arrays.fill(trendLevels, trendLevel);
        return new InitializingScoreTrend(trendLevels);
    }

    private static String buildTrendPattern(int levelsSize) {
        return Stream.generate(InitializingScoreTrendLevel.ANY::name)
                .limit(levelsSize)
                .collect(Collectors.joining("/"));
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public int getLevelsSize() {
        return trendLevels.length;
    }

    public boolean isOnlyUp() {
        for (var trendLevel : trendLevels) {
            if (trendLevel != InitializingScoreTrendLevel.ONLY_UP) {
                return false;
            }
        }
        return true;
    }

    public boolean isOnlyDown() {
        for (var trendLevel : trendLevels) {
            if (trendLevel != InitializingScoreTrendLevel.ONLY_DOWN) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InitializingScoreTrend that
                && Arrays.equals(trendLevels, that.trendLevels);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(trendLevels);
    }

    @Override
    public String toString() {
        return "InitializingScoreTrend(%s)".formatted(Arrays.toString(trendLevels));
    }
}
