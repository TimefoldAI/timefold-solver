package ai.timefold.solver.core.api.score.buildin.bendablelong;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.buildin.BendableLongScoreDefinition;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on n levels of long constraints.
 * The number of levels is bendable at configuration time.
 * <p>
 * This class is immutable.
 * <p>
 * The {@link #hardLevelsSize()} and {@link #softLevelsSize()} must be the same as in the
 * {@link BendableLongScoreDefinition} used.
 *
 * @see Score
 */
@NullMarked
public final class BendableLongScore implements IBendableScore<BendableLongScore> {

    public static BendableLongScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableLongScore.class, scoreString);
        var hardScores = new long[scoreTokens[0].length];
        for (var i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsLong(BendableLongScore.class, scoreString, scoreTokens[0][i]);
        }
        var softScores = new long[scoreTokens[1].length];
        for (var i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsLong(BendableLongScore.class, scoreString, scoreTokens[1][i]);
        }
        return of(hardScores, softScores);
    }

    /**
     * @deprecated Use {@link #of(long[], long[])} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static BendableLongScore ofUninitialized(int initScore, long[] hardScores, long[] softScores) {
        return BendableLongScore.of(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableLongScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static BendableLongScore of(long[] hardScores, long[] softScores) {
        return new BendableLongScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableLongScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static BendableLongScore zero(int hardLevelsSize, int softLevelsSize) {
        return new BendableLongScore(new long[hardLevelsSize], new long[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableLongScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param hardLevel at least 0, less than hardLevelsSize
     * @param hardScore any
     */
    public static BendableLongScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel, long hardScore) {
        var hardScores = new long[hardLevelsSize];
        hardScores[hardLevel] = hardScore;
        return new BendableLongScore(hardScores, new long[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableLongScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param softLevel at least 0, less than softLevelsSize
     * @param softScore any
     */
    public static BendableLongScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel, long softScore) {
        var softScores = new long[softLevelsSize];
        softScores[softLevel] = softScore;
        return new BendableLongScore(new long[hardLevelsSize], softScores);
    }

    private final long[] hardScores;
    private final long[] softScores;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private BendableLongScore() {
        this(new long[] {}, new long[] {});
    }

    /**
     *
     */
    private BendableLongScore(long[] hardScores, long[] softScores) {
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    /**
     * @return array copy because this class is immutable
     */
    public long[] hardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * As defined by {@link #hardScores()}.
     *
     * @deprecated Use {@link #hardScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public long[] getHardScores() {
        return hardScores();
    }

    /**
     * @return array copy because this class is immutable
     */
    public long[] softScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    /**
     * As defined by {@link #softScores()}.
     *
     * @deprecated Use {@link #softScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public long[] getSoftScores() {
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
    public long hardScore(int index) {
        return hardScores[index];
    }

    /**
     * As defined by {@link #hardScore(int)}.
     *
     * @deprecated Use {@link #hardScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public long getHardScore(int index) {
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
    public long softScore(int index) {
        return softScores[index];
    }

    /**
     * As defined by {@link #softScore(int)}.
     *
     * @deprecated Use {@link #softScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public long getSoftScore(int index) {
        return softScore(index);
    }

    /**
     * @param index {@code 0 <= index <} {@link #levelsSize()}
     * @return higher is better
     */
    public long hardOrSoftScore(int index) {
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
    public long getHardOrSoftScore(int index) {
        return hardOrSoftScore(index);
    }

    @Override
    public boolean isFeasible() {
        for (var hardScore : hardScores) {
            if (hardScore < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BendableLongScore add(BendableLongScore addend) {
        validateCompatible(addend);
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] + addend.hardScore(i);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] + addend.softScore(i);
        }
        return new BendableLongScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore subtract(BendableLongScore subtrahend) {
        validateCompatible(subtrahend);
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] - subtrahend.hardScore(i);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] - subtrahend.softScore(i);
        }
        return new BendableLongScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore multiply(double multiplicand) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(hardScores[i] * multiplicand);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(softScores[i] * multiplicand);
        }
        return new BendableLongScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore divide(double divisor) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(hardScores[i] / divisor);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(softScores[i] / divisor);
        }
        return new BendableLongScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore power(double exponent) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(Math.pow(hardScores[i], exponent));
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(Math.pow(softScores[i], exponent));
        }
        return new BendableLongScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore negate() { // Overridden as the default impl would create zero() all the time.
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = -hardScores[i];
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = -softScores[i];
        }
        return new BendableLongScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore abs() {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = Math.abs(hardScores[i]);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = Math.abs(softScores[i]);
        }
        return new BendableLongScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableLongScore zero() {
        return BendableLongScore.zero(hardLevelsSize(), softLevelsSize());
    }

    @Override
    public Number[] toLevelNumbers() {
        var levelNumbers = new Number[hardScores.length + softScores.length];
        for (var i = 0; i < hardScores.length; i++) {
            levelNumbers[i] = hardScores[i];
        }
        for (var i = 0; i < softScores.length; i++) {
            levelNumbers[hardScores.length + i] = softScores[i];
        }
        return levelNumbers;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BendableLongScore other) {
            if (hardLevelsSize() != other.hardLevelsSize()
                    || softLevelsSize() != other.softLevelsSize()) {
                return false;
            }
            for (var i = 0; i < hardScores.length; i++) {
                if (hardScores[i] != other.hardScore(i)) {
                    return false;
                }
            }
            for (var i = 0; i < softScores.length; i++) {
                if (softScores[i] != other.softScore(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(hardScores), Arrays.hashCode(softScores));
    }

    @Override
    public int compareTo(BendableLongScore other) {
        validateCompatible(other);
        for (var i = 0; i < hardScores.length; i++) {
            if (hardScores[i] != other.hardScore(i)) {
                return Long.compare(hardScores[i], other.hardScore(i));
            }
        }
        for (var i = 0; i < softScores.length; i++) {
            if (softScores[i] != other.softScore(i)) {
                return Long.compare(softScores[i], other.softScore(i));
            }
        }
        return 0;
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildBendableShortString(this, n -> n.longValue() != 0L);
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

    public void validateCompatible(BendableLongScore other) {
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
