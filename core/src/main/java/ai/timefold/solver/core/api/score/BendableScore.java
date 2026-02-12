package ai.timefold.solver.core.api.score;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.definition.BendableScoreDefinition;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on n levels of long constraints.
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
public record BendableScore(long[] hardScores, long[] softScores) implements IBendableScore<BendableScore> {

    public static BendableScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableScore.class, scoreString);
        var hardScores = new long[scoreTokens[0].length];
        for (var i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsLong(BendableScore.class, scoreString, scoreTokens[0][i]);
        }
        var softScores = new long[scoreTokens[1].length];
        for (var i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsLong(BendableScore.class, scoreString, scoreTokens[1][i]);
        }
        return of(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static BendableScore of(long[] hardScores, long[] softScores) {
        return new BendableScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static BendableScore zero(int hardLevelsSize, int softLevelsSize) {
        return new BendableScore(new long[hardLevelsSize], new long[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param hardLevel at least 0, less than hardLevelsSize
     * @param hardScore any
     */
    public static BendableScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel, long hardScore) {
        var hardScores = new long[hardLevelsSize];
        hardScores[hardLevel] = hardScore;
        return new BendableScore(hardScores, new long[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param softLevel at least 0, less than softLevelsSize
     * @param softScore any
     */
    public static BendableScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel, long softScore) {
        var softScores = new long[softLevelsSize];
        softScores[softLevel] = softScore;
        return new BendableScore(new long[hardLevelsSize], softScores);
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
    public BendableScore add(BendableScore addend) {
        validateCompatible(addend);
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] + addend.hardScore(i);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] + addend.softScore(i);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore subtract(BendableScore subtrahend) {
        validateCompatible(subtrahend);
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] - subtrahend.hardScore(i);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] - subtrahend.softScore(i);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore multiply(double multiplicand) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(hardScores[i] * multiplicand);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(softScores[i] * multiplicand);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore divide(double divisor) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(hardScores[i] / divisor);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(softScores[i] / divisor);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore power(double exponent) {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (long) Math.floor(Math.pow(hardScores[i], exponent));
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (long) Math.floor(Math.pow(softScores[i], exponent));
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore negate() { // Overridden as the default impl would create zero() all the time.
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = -hardScores[i];
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = -softScores[i];
        }
        return new BendableScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableScore abs() {
        var newHardScores = new long[hardScores.length];
        var newSoftScores = new long[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = Math.abs(hardScores[i]);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = Math.abs(softScores[i]);
        }
        return new BendableScore(newHardScores, newSoftScores);
    }

    @Override
    public BendableScore zero() {
        return BendableScore.zero(hardLevelsSize(), softLevelsSize());
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
        if (o instanceof BendableScore other) {
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
    public int compareTo(BendableScore other) {
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

    public void validateCompatible(BendableScore other) {
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
