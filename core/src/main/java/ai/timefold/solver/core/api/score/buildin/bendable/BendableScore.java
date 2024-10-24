package ai.timefold.solver.core.api.score.buildin.bendable;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.IBendableScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;
import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;

import org.jspecify.annotations.NonNull;

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
public final class BendableScore implements IBendableScore<BendableScore> {

    public static @NonNull BendableScore parseScore(@NonNull String scoreString) {
        String[][] scoreTokens = ScoreUtil.parseBendableScoreTokens(BendableScore.class, scoreString);
        int initScore = ScoreUtil.parseInitScore(BendableScore.class, scoreString, scoreTokens[0][0]);
        int[] hardScores = new int[scoreTokens[1].length];
        for (int i = 0; i < hardScores.length; i++) {
            hardScores[i] = ScoreUtil.parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[1][i]);
        }
        int[] softScores = new int[scoreTokens[2].length];
        for (int i = 0; i < softScores.length; i++) {
            softScores[i] = ScoreUtil.parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[2][i]);
        }
        return ofUninitialized(initScore, hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param initScore see {@link Score#initScore()}
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static @NonNull BendableScore ofUninitialized(int initScore, int @NonNull [] hardScores,
            int @NonNull [] softScores) {
        return new BendableScore(initScore, hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardScores never change that array afterwards: it must be immutable
     * @param softScores never change that array afterwards: it must be immutable
     */
    public static @NonNull BendableScore of(int @NonNull [] hardScores, int @NonNull [] softScores) {
        return new BendableScore(0, hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     */
    public static @NonNull BendableScore zero(int hardLevelsSize, int softLevelsSize) {
        return new BendableScore(0, new int[hardLevelsSize], new int[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param hardLevel at least 0, less than hardLevelsSize
     * @param hardScore any
     */
    public static @NonNull BendableScore ofHard(int hardLevelsSize, int softLevelsSize, int hardLevel, int hardScore) {
        int[] hardScores = new int[hardLevelsSize];
        hardScores[hardLevel] = hardScore;
        return new BendableScore(0, hardScores, new int[softLevelsSize]);
    }

    /**
     * Creates a new {@link BendableScore}.
     *
     * @param hardLevelsSize at least 0
     * @param softLevelsSize at least 0
     * @param softLevel at least 0, less than softLevelsSize
     * @param softScore any
     */
    public static @NonNull BendableScore ofSoft(int hardLevelsSize, int softLevelsSize, int softLevel, int softScore) {
        int[] softScores = new int[softLevelsSize];
        softScores[softLevel] = softScore;
        return new BendableScore(0, new int[hardLevelsSize], softScores);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final int[] hardScores;
    private final int[] softScores;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private BendableScore() {
        this(Integer.MIN_VALUE, new int[] {}, new int[] {});
    }

    /**
     * @param initScore see {@link Score#initScore()}
     */
    private BendableScore(int initScore, int @NonNull [] hardScores, int @NonNull [] softScores) {
        this.initScore = initScore;
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    @Override
    public int initScore() {
        return initScore;
    }

    /**
     * @return array copy because this class is immutable
     */
    public int @NonNull [] hardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * As defined by {@link #hardScores()}.
     *
     * @deprecated Use {@link #hardScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public int @NonNull [] getHardScores() {
        return hardScores();
    }

    /**
     * @return array copy because this class is immutable
     */
    public int @NonNull [] softScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    /**
     * As defined by {@link #softScores()}.
     *
     * @deprecated Use {@link #softScores()} instead.
     */
    @Deprecated(forRemoval = true)
    public int @NonNull [] getSoftScores() {
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

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public @NonNull BendableScore withInitScore(int newInitScore) {
        return new BendableScore(newInitScore, hardScores, softScores);
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
        if (initScore < 0) {
            return false;
        }
        for (int hardScore : hardScores) {
            if (hardScore < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NonNull BendableScore add(@NonNull BendableScore addend) {
        validateCompatible(addend);
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] + addend.hardScore(i);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] + addend.softScore(i);
        }
        return new BendableScore(
                initScore + addend.initScore(),
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore subtract(@NonNull BendableScore subtrahend) {
        validateCompatible(subtrahend);
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] - subtrahend.hardScore(i);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] - subtrahend.softScore(i);
        }
        return new BendableScore(
                initScore - subtrahend.initScore(),
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore multiply(double multiplicand) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] * multiplicand);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] * multiplicand);
        }
        return new BendableScore(
                (int) Math.floor(initScore * multiplicand),
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore divide(double divisor) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] / divisor);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] / divisor);
        }
        return new BendableScore(
                (int) Math.floor(initScore / divisor),
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore power(double exponent) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(Math.pow(hardScores[i], exponent));
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(Math.pow(softScores[i], exponent));
        }
        return new BendableScore(
                (int) Math.floor(Math.pow(initScore, exponent)),
                newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore negate() { // Overridden as the default impl would create zero() all the time.
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = -hardScores[i];
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = -softScores[i];
        }
        return new BendableScore(-initScore, newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore abs() {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = Math.abs(hardScores[i]);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = Math.abs(softScores[i]);
        }
        return new BendableScore(Math.abs(initScore), newHardScores, newSoftScores);
    }

    @Override
    public @NonNull BendableScore zero() {
        return BendableScore.zero(hardLevelsSize(), softLevelsSize());
    }

    @Override
    public Number @NonNull [] toLevelNumbers() {
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
        if (o instanceof BendableScore other) {
            if (hardLevelsSize() != other.hardLevelsSize()
                    || softLevelsSize() != other.softLevelsSize()) {
                return false;
            }
            if (initScore != other.initScore()) {
                return false;
            }
            for (int i = 0; i < hardScores.length; i++) {
                if (hardScores[i] != other.hardScore(i)) {
                    return false;
                }
            }
            for (int i = 0; i < softScores.length; i++) {
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
        return Objects.hash(initScore, Arrays.hashCode(hardScores), Arrays.hashCode(softScores));
    }

    @Override
    public int compareTo(@NonNull BendableScore other) {
        validateCompatible(other);
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        }
        for (int i = 0; i < hardScores.length; i++) {
            if (hardScores[i] != other.hardScore(i)) {
                return Integer.compare(hardScores[i], other.hardScore(i));
            }
        }
        for (int i = 0; i < softScores.length; i++) {
            if (softScores[i] != other.softScore(i)) {
                return Integer.compare(softScores[i], other.softScore(i));
            }
        }
        return 0;
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildBendableShortString(this, n -> n.intValue() != 0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(((hardScores.length + softScores.length) * 4) + 13);
        s.append(ScoreUtil.getInitPrefix(initScore));
        s.append("[");
        boolean first = true;
        for (int hardScore : hardScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(hardScore);
        }
        s.append("]hard/[");
        first = true;
        for (int softScore : softScores) {
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
