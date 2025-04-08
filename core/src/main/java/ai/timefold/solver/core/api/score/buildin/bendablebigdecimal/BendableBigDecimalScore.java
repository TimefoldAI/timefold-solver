package ai.timefold.solver.core.api.score.buildin.bendablebigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;

import org.jspecify.annotations.NullMarked;

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
@NullMarked
public final class BendableBigDecimalScore implements IBendableScore<BendableBigDecimalScore> {

    public static BendableBigDecimalScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableBigDecimalScore.class, scoreString);
        var hardScores = new BigDecimal[scoreTokens[0].length];
        for (var i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsBigDecimal(BendableBigDecimalScore.class, scoreString, scoreTokens[0][i]);
        }
        var softScores = new BigDecimal[scoreTokens[1].length];
        for (var i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsBigDecimal(BendableBigDecimalScore.class, scoreString, scoreTokens[1][i]);
        }
        return of(hardScores, softScores);
    }

    /**
     * @deprecated Use {@link #of(BigDecimal[], BigDecimal[])} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static BendableBigDecimalScore ofUninitialized(int initScore, BigDecimal[] hardScores, BigDecimal[] softScores) {
        return BendableBigDecimalScore.of(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static BendableBigDecimalScore of(BigDecimal[] hardScores, BigDecimal[] softScores) {
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableBigDecimalScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static BendableBigDecimalScore zero(int hardLevelsSize, int softLevelsSize) {
        var hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        var softScores = new BigDecimal[softLevelsSize];
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
    public static BendableBigDecimalScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel, BigDecimal hardScore) {
        var hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        var softScores = new BigDecimal[softLevelsSize];
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
    public static BendableBigDecimalScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel, BigDecimal softScore) {
        var hardScores = new BigDecimal[hardLevelsSize];
        Arrays.fill(hardScores, BigDecimal.ZERO);
        var softScores = new BigDecimal[softLevelsSize];
        Arrays.fill(softScores, BigDecimal.ZERO);
        softScores[softLevel] = softScore;
        return new BendableBigDecimalScore(hardScores, softScores);
    }

    private final BigDecimal[] hardScores;
    private final BigDecimal[] softScores;

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
    private BendableBigDecimalScore(BigDecimal[] hardScores, BigDecimal[] softScores) {
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    /**
     * @return array copy because this class is immutable
     */
    public BigDecimal[] hardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * As defined by {@link #hardScores()}.
     *
     * @deprecated Use {@link #hardScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal[] getHardScores() {
        return hardScores();
    }

    /**
     * @return array copy because this class is immutable
     */
    public BigDecimal[] softScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    /**
     * As defined by {@link #softScores()}.
     *
     * @deprecated Use {@link #softScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal[] getSoftScores() {
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
    public BigDecimal hardScore(int index) {
        return hardScores[index];
    }

    /**
     * As defined by {@link #hardScore(int)}.
     *
     * @deprecated Use {@link #hardScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getHardScore(int index) {
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
    public BigDecimal softScore(int index) {
        return softScores[index];
    }

    /**
     * As defined by {@link #softScore(int)}.
     *
     * @deprecated Use {@link #softScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getSoftScore(int index) {
        return softScore(index);
    }

    /**
     * @param index {@code 0 <= index <} {@link #levelsSize()}
     * @return higher is better
     */
    public BigDecimal hardOrSoftScore(int index) {
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
    public BigDecimal getHardOrSoftScore(int index) {
        return hardOrSoftScore(index);
    }

    @Override
    public boolean isFeasible() {
        for (var hardScore : hardScores) {
            if (hardScore.compareTo(BigDecimal.ZERO) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BendableBigDecimalScore add(BendableBigDecimalScore addend) {
        validateCompatible(addend);
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].add(addend.hardScore(i));
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].add(addend.softScore(i));
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore subtract(BendableBigDecimalScore subtrahend) {
        validateCompatible(subtrahend);
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].subtract(subtrahend.hardScore(i));
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].subtract(subtrahend.softScore(i));
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore multiply(double multiplicand) {
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        var bigDecimalMultiplicand = BigDecimal.valueOf(multiplicand);
        for (var i = 0; i < newHardScores.length; i++) {
            // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
            newHardScores[i] = hardScores[i].multiply(bigDecimalMultiplicand).setScale(hardScores[i].scale(),
                    RoundingMode.FLOOR);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
            newSoftScores[i] = softScores[i].multiply(bigDecimalMultiplicand).setScale(softScores[i].scale(),
                    RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore divide(double divisor) {
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        var bigDecimalDivisor = BigDecimal.valueOf(divisor);
        for (var i = 0; i < newHardScores.length; i++) {
            var hardScore = hardScores[i];
            newHardScores[i] = hardScore.divide(bigDecimalDivisor, hardScore.scale(), RoundingMode.FLOOR);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            var softScore = softScores[i];
            newSoftScores[i] = softScore.divide(bigDecimalDivisor, softScore.scale(), RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore power(double exponent) {
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        var actualExponent = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        for (var i = 0; i < newHardScores.length; i++) {
            var hardScore = hardScores[i];
            newHardScores[i] = hardScore.pow(actualExponent.intValue()).setScale(hardScore.scale(), RoundingMode.FLOOR);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            var softScore = softScores[i];
            newSoftScores[i] = softScore.pow(actualExponent.intValue()).setScale(softScore.scale(), RoundingMode.FLOOR);
        }
        return new BendableBigDecimalScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore negate() { // Overridden as the default impl would create zero() all the time.
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].negate();
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].negate();
        }
        return new BendableBigDecimalScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore abs() {
        var newHardScores = new BigDecimal[hardScores.length];
        var newSoftScores = new BigDecimal[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i].abs();
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i].abs();
        }
        return new BendableBigDecimalScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableBigDecimalScore zero() {
        return BendableBigDecimalScore.zero(hardLevelsSize(), softLevelsSize());
    }

    @Override
    public Number[] toLevelNumbers() {
        var levelNumbers = new Number[hardScores.length + softScores.length];
        System.arraycopy(hardScores, 0, levelNumbers, 0, hardScores.length);
        System.arraycopy(softScores, 0, levelNumbers, hardScores.length, softScores.length);
        return levelNumbers;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BendableBigDecimalScore other) {
            if (hardLevelsSize() != other.hardLevelsSize()
                    || softLevelsSize() != other.softLevelsSize()) {
                return false;
            }
            for (var i = 0; i < hardScores.length; i++) {
                if (!hardScores[i].stripTrailingZeros().equals(other.hardScore(i).stripTrailingZeros())) {
                    return false;
                }
            }
            for (var i = 0; i < softScores.length; i++) {
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
        var scoreHashCodes = Stream.concat(Arrays.stream(hardScores), Arrays.stream(softScores))
                .map(BigDecimal::stripTrailingZeros)
                .mapToInt(BigDecimal::hashCode)
                .toArray();
        return Arrays.hashCode(scoreHashCodes);
    }

    @Override
    public int compareTo(BendableBigDecimalScore other) {
        validateCompatible(other);
        for (var i = 0; i < hardScores.length; i++) {
            var hardScoreComparison = hardScores[i].compareTo(other.hardScore(i));
            if (hardScoreComparison != 0) {
                return hardScoreComparison;
            }
        }
        for (var i = 0; i < softScores.length; i++) {
            var softScoreComparison = softScores[i].compareTo(other.softScore(i));
            if (softScoreComparison != 0) {
                return softScoreComparison;
            }
        }
        return 0;
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildBendableShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0);
    }

    @Override
    public String toString() {
        var s = new StringBuilder(((hardScores.length + softScores.length) * 4) + 7);
        s.append("[");
        var first = true;
        for (var hardScore : hardScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(hardScore);
        }
        s.append("]hard/[");
        first = true;
        for (var softScore : softScores) {
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
