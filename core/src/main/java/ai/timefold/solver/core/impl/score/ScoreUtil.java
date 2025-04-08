package ai.timefold.solver.core.impl.score;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;

public final class ScoreUtil {

    public static final String HARD_LABEL = "hard";
    public static final String MEDIUM_LABEL = "medium";
    public static final String SOFT_LABEL = "soft";
    public static final String[] LEVEL_SUFFIXES = new String[] { HARD_LABEL, SOFT_LABEL };

    public static String[] parseScoreTokens(Class<? extends Score<?>> scoreClass, String scoreString, String... levelSuffixes) {
        var scoreTokens = new String[levelSuffixes.length];
        var suffixedScoreTokens = scoreString.split("/");
        if (suffixedScoreTokens.length != levelSuffixes.length) {
            throw new IllegalArgumentException("""
                    The scoreString (%s) for the scoreClass (%s) doesn't follow the correct pattern (%s): \
                    the suffixedScoreTokens length (%d) differs from the levelSuffixes length (%d or %d)."""
                    .formatted(scoreString, scoreClass.getSimpleName(), buildScorePattern(false, levelSuffixes),
                            suffixedScoreTokens.length, levelSuffixes.length, levelSuffixes.length + 1));
        }
        for (var i = 0; i < levelSuffixes.length; i++) {
            var suffixedScoreToken = suffixedScoreTokens[i];
            var levelSuffix = levelSuffixes[i];
            if (!suffixedScoreToken.endsWith(levelSuffix)) {
                throw new IllegalArgumentException("""
                        The scoreString (%s) for the scoreClass (%s) doesn't follow the correct pattern (%s): \
                        the suffixedScoreToken (%s) does not end with levelSuffix (%s)."""
                        .formatted(scoreString, scoreClass.getSimpleName(), buildScorePattern(false, levelSuffixes),
                                suffixedScoreToken, levelSuffix));
            }
            scoreTokens[i] = suffixedScoreToken.substring(0, suffixedScoreToken.length() - levelSuffix.length());
        }
        return scoreTokens;
    }

    public static int parseLevelAsInt(Class<? extends Score<?>> scoreClass, String scoreString, String levelString) {
        if (levelString.equals("*")) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "The scoreString (%s) for the scoreClass (%s) has a levelString (%s) which is not a valid integer."
                            .formatted(scoreString, scoreClass.getSimpleName(), levelString),
                    e);
        }
    }

    public static long parseLevelAsLong(Class<? extends Score<?>> scoreClass, String scoreString, String levelString) {
        if (levelString.equals("*")) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(levelString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "The scoreString (%s) for the scoreClass (%s) has a levelString (%s) which is not a valid long."
                            .formatted(scoreString, scoreClass.getSimpleName(), levelString),
                    e);
        }
    }

    public static BigDecimal parseLevelAsBigDecimal(Class<? extends Score<?>> scoreClass, String scoreString,
            String levelString) {
        if (levelString.equals("*")) {
            throw new IllegalArgumentException("""
                    The scoreString (%s) for the scoreClass (%s) has a wildcard (*) as levelString (%s) \
                    which is not supported for BigDecimal score values, because there is no general MIN_VALUE for BigDecimal."""
                    .formatted(scoreString, scoreClass.getSimpleName(), levelString));
        }
        try {
            return new BigDecimal(levelString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "The scoreString (%s) for the scoreClass (%s) has a levelString (%s) which is not a valid BigDecimal."
                            .formatted(scoreString, scoreClass.getSimpleName(), levelString),
                    e);
        }
    }

    public static String buildScorePattern(boolean bendable, String... levelSuffixes) {
        var scorePattern = new StringBuilder(levelSuffixes.length * 10);
        var first = true;
        for (var levelSuffix : levelSuffixes) {
            if (first) {
                first = false;
            } else {
                scorePattern.append("/");
            }
            if (bendable) {
                scorePattern.append("[999/.../999]");
            } else {
                scorePattern.append("999");
            }
            scorePattern.append(levelSuffix);
        }
        return scorePattern.toString();
    }

    public static <Score_ extends Score<Score_>> String buildShortString(Score<Score_> score, Predicate<Number> notZero,
            String... levelLabels) {
        var shortString = new StringBuilder();
        var i = 0;
        for (var levelNumber : score.toLevelNumbers()) {
            if (notZero.test(levelNumber)) {
                if (!shortString.isEmpty()) {
                    shortString.append("/");
                }
                shortString.append(levelNumber).append(levelLabels[i]);
            }
            i++;
        }
        if (shortString.isEmpty()) {
            // Even for BigDecimals we use "0" over "0.0" because different levels can have different scales
            return "0";
        }
        return shortString.toString();
    }

    public static String[][] parseBendableScoreTokens(Class<? extends IBendableScore<?>> scoreClass, String scoreString) {
        var scoreTokens = new String[2][];
        var startIndex = 0;
        for (var i = 0; i < LEVEL_SUFFIXES.length; i++) {
            var levelSuffix = LEVEL_SUFFIXES[i];
            var endIndex = scoreString.indexOf(levelSuffix, startIndex);
            if (endIndex < 0) {
                throw new IllegalArgumentException("""
                        The scoreString (%s) for the scoreClass (%s) doesn't follow the correct pattern (%s): \
                        the levelSuffix (%s) isn't in the scoreSubstring (%s)."""
                        .formatted(scoreString, scoreClass.getSimpleName(), buildScorePattern(true, LEVEL_SUFFIXES),
                                levelSuffix, scoreString.substring(startIndex)));
            }
            var scoreSubString = scoreString.substring(startIndex, endIndex);
            if (!scoreSubString.startsWith("[") || !scoreSubString.endsWith("]")) {
                throw new IllegalArgumentException("""
                        The scoreString (%s) for the scoreClass (%s) doesn't follow the correct pattern (%s): \
                        the scoreSubString (%s) does not start and end with "[" and "]"."""
                        .formatted(scoreString, scoreClass.getSimpleName(), buildScorePattern(true, LEVEL_SUFFIXES),
                                scoreString));
            }
            scoreTokens[i] = scoreSubString.equals("[]") ? new String[0]
                    : scoreSubString.substring(1, scoreSubString.length() - 1).split("/");
            startIndex = endIndex + levelSuffix.length() + "/".length();
        }
        if (startIndex != scoreString.length() + "/".length()) {
            throw new IllegalArgumentException("""
                    The scoreString (%s) for the scoreClass (%s) doesn't follow the correct pattern (%s): \
                    the suffix (%s) is unsupported."""
                    .formatted(scoreString, scoreClass.getSimpleName(), buildScorePattern(true, LEVEL_SUFFIXES),
                            scoreString.substring(startIndex - 1)));
        }
        return scoreTokens;
    }

    public static <Score_ extends IBendableScore<Score_>> String buildBendableShortString(IBendableScore<Score_> score,
            Predicate<Number> notZero) {
        var shortString = new StringBuilder();
        var levelNumbers = score.toLevelNumbers();
        var hardLevelsSize = score.hardLevelsSize();
        if (Arrays.stream(levelNumbers).limit(hardLevelsSize).anyMatch(notZero)) {
            if (!shortString.isEmpty()) {
                shortString.append("/");
            }
            shortString.append("[");
            var first = true;
            for (var i = 0; i < hardLevelsSize; i++) {
                if (first) {
                    first = false;
                } else {
                    shortString.append("/");
                }
                shortString.append(levelNumbers[i]);
            }
            shortString.append("]").append(HARD_LABEL);
        }
        var softLevelsSize = score.softLevelsSize();
        if (Arrays.stream(levelNumbers).skip(hardLevelsSize).anyMatch(notZero)) {
            if (!shortString.isEmpty()) {
                shortString.append("/");
            }
            shortString.append("[");
            var first = true;
            for (var i = 0; i < softLevelsSize; i++) {
                if (first) {
                    first = false;
                } else {
                    shortString.append("/");
                }
                shortString.append(levelNumbers[hardLevelsSize + i]);
            }
            shortString.append("]").append(SOFT_LABEL);
        }
        if (shortString.isEmpty()) {
            // Even for BigDecimals we use "0" over "0.0" because different levels can have different scales
            return "0";
        }
        return shortString.toString();
    }

    private ScoreUtil() {
        // No external instances.
    }

}
