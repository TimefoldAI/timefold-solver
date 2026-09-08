package ai.timefold.solver.core.impl.score;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;

public final class ScoreUtil {

    public static final String STRUCTURAL_LABEL = "structural";
    public static final String HARD_LABEL = "hard";
    public static final String MEDIUM_LABEL = "medium";
    public static final String SOFT_LABEL = "soft";
    public static final String[] LEVEL_SUFFIXES = new String[] { HARD_LABEL, SOFT_LABEL };
    /**
     * For {@link #buildShortString} and {@link #buildBendableShortString}
     * of a BigDecimal-based score.
     *
     * @see #isZero(BigDecimal)
     */
    public static final Predicate<Number> BIG_DECIMAL_NOT_ZERO = n -> !isZero((BigDecimal) n);

    public static String[] parseScoreTokens(Class<? extends Score<?>> scoreClass, String scoreString, String... levelSuffixes) {
        var scoreTokens = new String[levelSuffixes.length];
        var suffixedScoreTokens = scoreString.split("/");
        if (suffixedScoreTokens.length == levelSuffixes.length + 1) {
            var newLevelSuffixes = Arrays.copyOf(levelSuffixes, levelSuffixes.length + 1);
            System.arraycopy(levelSuffixes, 0, newLevelSuffixes, 1, levelSuffixes.length);
            newLevelSuffixes[0] = STRUCTURAL_LABEL;
            scoreTokens = new String[levelSuffixes.length + 1];
            levelSuffixes = newLevelSuffixes;
        }
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
        if (score.structuralScore() != 0L) {
            shortString.append(score.structuralScore()).append(STRUCTURAL_LABEL);
        }
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
        var scoreTokens = new String[3][];
        var startIndex = 0;
        var structuralSlashIndex = scoreString.indexOf(STRUCTURAL_LABEL + "/");
        if (structuralSlashIndex >= 0) {
            scoreTokens[0] = new String[] { scoreString.substring(0, structuralSlashIndex) };
            startIndex = structuralSlashIndex + STRUCTURAL_LABEL.length() + 1;
        }
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
            scoreTokens[i + 1] = scoreSubString.equals("[]") ? new String[0]
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
        if (score.structuralScore() != 0L) {
            shortString.append(score.structuralScore()).append(STRUCTURAL_LABEL);
        }
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

    /**
     * Tests if a {@link BigDecimal} score level is numerically zero, at any scale.
     * <p>
     * Never use {@link BigDecimal#equals(Object)} for this test.
     * {@code equals()} also compares the scale,
     * so {@code new BigDecimal("0.00")} is not equal to {@link BigDecimal#ZERO},
     * although both are zero.
     * <p>
     * Zero is the only value that a score factory is permitted to replace with a shared constant.
     * A shared constant has scale 0, and zero keeps its meaning at every scale,
     * because zero multiplied or divided stays zero.
     * Every other value must keep the scale that the caller supplied, because {@code multiply()},
     * {@code divide()} and {@code power()} take the scale of their result from the scale of the input.
     * If {@code SimpleBigDecimalScore.of(new BigDecimal("1.0"))} returned the shared {@code ONE}, which has scale 0,
     * then {@code multiply(1.2)} would give 1 instead of 1.2.
     *
     * @return true if the value is zero at any scale
     */
    public static boolean isZero(BigDecimal value) {
        return value.signum() == 0;
    }

    /**
     * Tests if a {@link BigDecimal} score level is negative, at any scale, for an {@code isFeasible()} check.
     * <p>
     * {@link BigDecimal#signum()} already handles scale and negative-zero forms correctly,
     * so unlike {@link #isZero} this only gives the sign test one spelling.
     * Writing it with {@code compareTo()} would not be a bug.
     *
     * @return true if the value is less than zero
     */
    public static boolean isNegative(BigDecimal value) {
        return value.signum() < 0;
    }

    /**
     * Tests two {@link BigDecimal} score levels for numerical equality,
     * ignoring the scale, so that 1.0 and 1.00 are equal.
     * <p>
     * This is what stops the scale from causing a false score corruption.
     * A score that a solver built one move at a time can carry a higher scale
     * than the same score calculated from scratch,
     * because {@link BigDecimal#add(BigDecimal)} keeps the larger scale of its two operands.
     * Both must still be equal, and {@code AbstractScoreDirector} compares them with {@code equals()}.
     * <p>
     * {@code toString()} keeps the scale, although this ignores it.
     * That difference is deliberate,
     * because {@code toString()} is also the persistence format that {@code parseScore()} reads back.
     * A score of 10.00 that printed as 10 would parse back at scale 0,
     * and {@code multiply(0.5)} would then give 5 instead of 5.00,
     * with nothing to report the loss.
     * <p>
     * Pair every use with {@link #hashCodeIgnoringScale}.
     *
     * @return true if both values are the same number, whatever their scale
     */
    public static boolean equalsIgnoringScale(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    /**
     * The hash of a {@link BigDecimal} score level that ignores the scale,
     * so that 1.0 and 1.00 hash alike.
     *
     * @see #equalsIgnoringScale which this must agree with
     */
    public static int hashCodeIgnoringScale(BigDecimal value) {
        return value.stripTrailingZeros().hashCode();
    }

    private ScoreUtil() {
        // No external instances.
    }

}
