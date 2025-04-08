package ai.timefold.solver.core.api.score.buildin.bendable;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on n levels of int constraints.
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
public final class BendableScore implements IBendableScore<BendableScore> {

    public static BendableScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableScore.class, scoreString);
        var hardScores = new int[scoreTokens[0].length];
        for (var i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[0][i]);
        }
        var softScores = new int[scoreTokens[1].length];
        for (var i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[1][i]);
        }
        return of(hardScores, softScores);
    }

    /**
     * @deprecated Use {@link #of(int[], int[])} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static BendableScore ofUninitialized(int initScore, int[] hardScores, int[] softScores) {
        return BendableScore.of(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static BendableScore of(int[] hardScores, int[] softScores) {
        return new BendableScore(hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static BendableScore zero(int hardLevelsSize, int softLevelsSize) {
        return new BendableScore(new int[hardLevelsSize], new int[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param hardLevel at least 0, less than hardLevelsSize
     * @param hardScore any
     */
    public static BendableScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel, int hardScore) {
        var hardScores = new int[hardLevelsSize];
        hardScores[hardLevel] = hardScore;
        return new BendableScore(hardScores, new int[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param softLevel at least 0, less than softLevelsSize
     * @param softScore any
     */
    public static BendableScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel, int softScore) {
        var softScores = new int[softLevelsSize];
        softScores[softLevel] = softScore;
        return new BendableScore(new int[hardLevelsSize], softScores);
    }

    private final int[] hardScores;
    private final int[] softScores;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private BendableScore() {
        this(new int[] {}, new int[] {});
    }

    /**
     *
     */
    private BendableScore(int[] hardScores, int[] softScores) {
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    /**
     * @return array copy because this class is immutable
     */
    public int[] hardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * As defined by {@link #hardScores()}.
     *
     * @deprecated Use {@link #hardScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public int[] getHardScores() {
        return hardScores();
    }

    /**
     * @return array copy because this class is immutable
     */
    public int[] softScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    /**
     * As defined by {@link #softScores()}.
     *
     * @deprecated Use {@link #softScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public int[] getSoftScores() {
        return softScores();
    }

    @Override
    public int hardLevelsSize() {
        return hardScores.length;
    }

    /**
     * @param hardLevel {@code 0 <= hardLevel <} {@link #hardLevelsSize()}.
     *        The {@code scoreLevel} is {@code hardLevel} for hard levels and {@code softLevel + hardLevelSize} for soft levels.
     * @return higher is better
     */
    public int hardScore(int hardLevel) {
        return hardScores[hardLevel];
    }

    /**
     * As defined by {@link #hardScore(int)}.
     *
     * @deprecated Use {@link #hardScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public int getHardScore(int hardLevel) {
        return hardScore(hardLevel);
    }

    @Override
    public int softLevelsSize() {
        return softScores.length;
    }

    /**
     * @param softLevel {@code 0 <= softLevel <} {@link #softLevelsSize()}.
     *        The {@code scoreLevel} is {@code hardLevel} for hard levels and {@code softLevel + hardLevelSize} for soft levels.
     * @return higher is better
     */
    public int softScore(int softLevel) {
        return softScores[softLevel];
    }

    /**
     * As defined by {@link #softScore(int)}.
     *
     * @deprecated Use {@link #softScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public int getSoftScore(int hardLevel) {
        return softScore(hardLevel);
    }

    /**
     * @param level {@code 0 <= level <} {@link #levelsSize()}
     * @return higher is better
     */
    public int hardOrSoftScore(int level) {
        if (level < hardScores.length) {
            return hardScores[level];
        } else {
            return softScores[level - hardScores.length];
        }
    }

    /**
     * As defined by {@link #hardOrSoftScore(int)}.
     *
     * @deprecated Use {@link #hardOrSoftScore(int)} instead.
     */
    @Deprecated(forRemoval = true)
    public int getHardOrSoftScore(int level) {
        return hardOrSoftScore(level);
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
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
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
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
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
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] * multiplicand);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] * multiplicand);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore divide(double divisor) {
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] / divisor);
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] / divisor);
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore power(double exponent) {
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
        for (var i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(Math.pow(hardScores[i], exponent));
        }
        for (var i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(Math.pow(softScores[i], exponent));
        }
        return new BendableScore(
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore negate() { // Overridden as the default impl would create zero() all the time.
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
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
        var newHardScores = new int[hardScores.length];
        var newSoftScores = new int[softScores.length];
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
                return Integer.compare(hardScores[i], other.hardScore(i));
            }
        }
        for (var i = 0; i < softScores.length; i++) {
            if (softScores[i] != other.softScore(i)) {
                return Integer.compare(softScores[i], other.softScore(i));
            }
        }
        return 0;
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildBendableShortString(this, n -> n.intValue() != 0);
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
