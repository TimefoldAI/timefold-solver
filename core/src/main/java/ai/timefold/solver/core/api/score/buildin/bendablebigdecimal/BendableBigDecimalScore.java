package ai.timefold.solver.core.api.score.buildin.bendablebigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on n levels of {@link BigDecimal} constraints.
 * The number of levels is bendable at configuration time.
 * <p>
 * This class is immutable.
 * <p>
 * The {@link #hardLevelsSize()} and {@link #softLevelsSize()} must be the same as in the
 * {@link BendableScoreDefinition} used.
 *
 * @see Score
 */
public final class BendableBigDecimalScore implements IBendableScore<BendableBigDecimalScore> {

    public static @NonNull BendableBigDecimalScore parseScore(@NonNull String scoreString) {
        String[][] scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableBigDecimalScore.class, scoreString);
        BigDecimal[] hardScores = new BigDecimal[scoreTokens[1].length];
        for (int i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsBigDecimal(BendableBigDecimalScore.class, scoreString, scoreTokens[1][i]);
        }
        BigDecimal[] softScores = new BigDecimal[scoreTokens[2].length];
        for (int i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsBigDecimal(BendableBigDecimalScore.class, scoreString, scoreTokens[2][i]);
        }
        return of(hardScores, softScores);
    }

    /**
     * @deprecated Use {@link #of(BigDecimal[], BigDecimal[])} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.21.0")
    public static @NonNull BendableBigDecimalScore ofUninitialized(int initScore, @NonNull BigDecimal @NonNull [] hardScores,
            @NonNull BigDecimal @NonNull [] softScores) {
        return BendableBigDecimalScore.of(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static @NonNull BendableBigDecimalScore of(BigDecimal @NonNull [] hardScores, BigDecimal @NonNull [] softScores) {
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static @NonNull BendableBigDecimalScore zero(int hardLevelsSize, int softLevelsSize) {
        BigDecimal[] hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        BigDecimal[] softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param hardLevel at least 0, less than hardLevelsSize
     */
    public static @NonNull BendableBigDecimalScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel,
            @NonNull BigDecimal hardScore) {
        BigDecimal[] hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        BigDecimal[] softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
        hardScores[hardLevel] = hardScore;
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param softLevel at least 0, less than softLevelsSize
     */
    public static @NonNull BendableBigDecimalScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel,
            @NonNull BigDecimal softScore) {
        BigDecimal[] hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        BigDecimal[] softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
        softScores[softLevel] = softScore;
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    private final @NonNull BigDecimal @NonNull [] hardScores;
    private final @NonNull BigDecimal @NonNull [] softScores;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private BendableBigDecimalScore() {
        this(new BigDecimal[] {}, new BigDecimal[] {});
    }

    /**
     *
     */
    private BendableBigDecimalScore(@NonNull BigDecimal @NonNull [] hardScores, @NonNull BigDecimal @NonNull [] softScores) {
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    /**
     * @return array copy because this class is immutable
     */
    public @NonNull BigDecimal @NonNull [] hardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * As defined by {@link #hardScores()}.
     *
     * @deprecated Use {@link #hardScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal @NonNull [] getHardScores() {
        return hardScores();
    }

    /**
     * @return array copy because this class is immutable
     */
    public @NonNull BigDecimal @NonNull [] softScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    /**
     * As defined by {@link #softScores()}.
     *
     * @deprecated Use {@link #softScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal @NonNull [] getSoftScores() {
        return softScores();
    }

    @Override
    public int hardLevelsSize() {
        return hardScores.length;
    }

    /**
     * @param index {@code 0 <= index <} {@link #hardLevelsSize()}
     * @return higher is better
     */
    public @NonNull BigDecimal hardScore(int index) {
        return hardScores[index];
    }

    /**
     * As defined by {@link #hardScore(int)}.
     *
     * @deprecated Use {@link #hardScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getHardScore(int index) {
        return hardScore(index);
    }

    @Override
    public int softLevelsSize() {
        return softScores.length;
    }

    /**
     * @param index {@code 0 <= index <} {@link #softLevelsSize()}
     * @return higher is better
     */
    public @NonNull BigDecimal softScore(int index) {
        return softScores[index];
    }

    /**
     * As defined by {@link #softScore(int)}.
     *
     * @deprecated Use {@link #softScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getSoftScore(int index) {
        return softScore(index);
    }

    /**
     * @param index {@code 0 <= index <} {@link #levelsSize()}
     * @return higher is better
     */
    public @NonNull BigDecimal hardOrSoftScore(int index) {
        if (index < hardScores.length) {
            return hardScores[index];
        } else {
            return softScores[index - hardScores.length];
        }
    }

    /**
     * As defined by {@link #hardOrSoftScore(int)}.
     *
     * @deprecated Use {@link #hardOrSoftScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getHardOrSoftScore(int index) {
        return hardOrSoftScore(index);
    }

    @Override
    public boolean isFeasible() {
        for (BigDecimal hardScore : hardScores) {
            if (hardScore.compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull BendableBigDecimalScore add(@NonNull BendableBigDecimalScore addend) {
        validateCompatible(addend);
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].add(addend.hardScore(i));
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].add(addend.softScore(i));
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore subtract(@NonNull BendableBigDecimalScore subtrahend) {
        validateCompatible(subtrahend);
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].subtract(subtrahend.hardScore(i));
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].subtract(subtrahend.softScore(i));
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore multiply(double multiplicand) {
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        BigDecimal bigDecimalMultiplicand = BigDecimal.valueOf(multiplicand);
        for (int i = 0; i < newHardScores.length; i++) {
            // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
            newHardScores[i] = hardScores[i].multiply(bigDecimalMultiplicand).setScale(hardScores[i].scale(),
                    RoundingMode.FLOOR);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
            newSoftScores[i] = softScores[i].multiply(bigDecimalMultiplicand).setScale(softScores[i].scale(),
                    RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore divide(double divisor) {
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        BigDecimal bigDecimalDivisor = BigDecimal.valueOf(divisor);
        for (int i = 0; i < newHardScores.length; i++) {
            BigDecimal hardScore = hardScores[i];
            newHardScores[i] = hardScore.divide(bigDecimalDivisor, hardScore.scale(), RoundingMode.FLOOR);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            BigDecimal softScore = softScores[i];
            newSoftScores[i] = softScore.divide(bigDecimalDivisor, softScore.scale(), RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore power(double exponent) {
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        BigDecimal actualExponent = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        for (int i = 0; i < newHardScores.length; i++) {
            BigDecimal hardScore = hardScores[i];
            newHardScores[i] = hardScore.pow(actualExponent.intValue()).setScale(hardScore.scale(), RoundingMode.FLOOR);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            BigDecimal softScore = softScores[i];
            newSoftScores[i] = softScore.pow(actualExponent.intValue()).setScale(softScore.scale(), RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore negate() { // Overridden as the default impl would create zero() all the time.
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].negate();
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].negate();
        }
        return new BendableBigDecimalScore(newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore abs() {
        BigDecimal[] newHardScores = new BigDecimal[hardScores.length];
        BigDecimal[] newSoftScores = new BigDecimal[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].abs();
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].abs();
        }
        return new BendableBigDecimalScore(newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableBigDecimalScore zero() {
        return BendableBigDecimalScore.zero(hardLevelsSize(), softLevelsSize());
    }

    @Override
    public @NonNull Number @NonNull [] toLevelNumbers() {
        Number[] levelNumbers = new Number[hardScores.length + softScores.length];
        for (int i = 0; i < hardScores.length; i++) {
            levelNumbers[i] = hardScores[i];
        }
        for (int i = 0; i < softScores.length; i++) {
            levelNumbers[hardScores.length + i] = softScores[i];
        }
        return levelNumbers;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BendableBigDecimalScore other) {
            if (hardLevelsSize() != other.hardLevelsSize()
                    || softLevelsSize() != other.softLevelsSize()) {
                return false;
            }
            for (int i = 0; i < hardScores.length; i++) {
                if (!hardScores[i].stripTrailingZeros().equals(other.hardScore(i).stripTrailingZeros())) {
                    return false;
                }
            }
            for (int i = 0; i < softScores.length; i++) {
                if (!softScores[i].stripTrailingZeros().equals(other.softScore(i).stripTrailingZeros())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int[] scoreHashCodes = Stream.concat(Arrays.stream(hardScores), Arrays.stream(softScores))
                .map(BigDecimal::stripTrailingZeros)
                .mapToInt(BigDecimal::hashCode)
                .toArray();
        return Arrays.hashCode(scoreHashCodes);
    }

    @Override
    public int compareTo(@NonNull BendableBigDecimalScore other) {
        validateCompatible(other);
        for (int i = 0; i < hardScores.length; i++) {
            int hardScoreComparison = hardScores[i].compareTo(other.hardScore(i));
            if (hardScoreComparison != 0) {
                return hardScoreComparison;
            }
        }
        for (int i = 0; i < softScores.length; i++) {
            int softScoreComparison = softScores[i].compareTo(other.softScore(i));
            if (softScoreComparison != 0) {
                return softScoreComparison;
            }
        }
        return 0;
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildBendableShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(((hardScores.length + softScores.length) * 4) + 7);
        s.append("[");
        boolean first = true;
        for (BigDecimal hardScore : hardScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(hardScore);
        }
        s.append("]hard/[");
        first = true;
        for (BigDecimal softScore : softScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(softScore);
        }
        s.append("]soft");
        return s.toString();
    }

    public void validateCompatible(BendableBigDecimalScore other) {
        if (hardLevelsSize() != other.hardLevelsSize()) {
            throw new IllegalArgumentException("The score (" + this
                    + ") with hardScoreSize (" + hardLevelsSize()
                    + ") is not compatible with the other score (" + other
                    + ") with hardScoreSize (" + other.hardLevelsSize() + ").");
        }
        if (softLevelsSize() != other.softLevelsSize()) {
            throw new IllegalArgumentException("The score (" + this
                    + ") with softScoreSize (" + softLevelsSize()
                    + ") is not compatible with the other score (" + other
                    + ") with softScoreSize (" + other.softLevelsSize() + ").");
        }
    }

}
