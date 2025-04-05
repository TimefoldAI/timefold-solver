package ai.timefold.solver.core.api.score.buildin.hardsoft;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.parseLevelAsInt;
import static ai.timefold.solver.core.impl.score.ScoreUtil.parseScoreTokens;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on 2 levels of int constraints: hard and soft.
 * Hard constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class HardSoftScore implements Score<HardSoftScore> {

    public static final @NonNull HardSoftScore ZERO = new HardSoftScore(0, 0);
    public static final @NonNull HardSoftScore ONE_HARD = new HardSoftScore(1, 0);
    public static final @NonNull HardSoftScore ONE_SOFT = new HardSoftScore(0, 1);
    private static final @NonNull HardSoftScore MINUS_ONE_SOFT = new HardSoftScore(0, -1);
    private static final @NonNull HardSoftScore MINUS_ONE_HARD = new HardSoftScore(-1, 0);

    public static @NonNull HardSoftScore parseScore(@NonNull String scoreString) {
        String[] scoreTokens = parseScoreTokens(HardSoftScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        int hardScore = parseLevelAsInt(HardSoftScore.class, scoreString, scoreTokens[1]);
        int softScore = parseLevelAsInt(HardSoftScore.class, scoreString, scoreTokens[2]);
        return of(hardScore, softScore);
    }

    /**
     * @deprecated Use {@link #of(int, int)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.21.0")
    public static @NonNull HardSoftScore ofUninitialized(int initScore, int hardScore, int softScore) {
        return of(hardScore, softScore);
    }

    public static @NonNull HardSoftScore of(int hardScore, int softScore) {
        // Optimization for frequently seen values.
        if (hardScore == 0) {
            if (softScore == -1) {
                return MINUS_ONE_SOFT;
            } else if (softScore == 0) {
                return ZERO;
            } else if (softScore == 1) {
                return ONE_SOFT;
            }
        } else if (softScore == 0) {
            if (hardScore == 1) {
                return ONE_HARD;
            } else if (hardScore == -1) {
                return MINUS_ONE_HARD;
            }
        }
        // Every other case is constructed.
        return new HardSoftScore(hardScore, softScore);
    }

    public static @NonNull HardSoftScore ofHard(int hardScore) {
        // Optimization for frequently seen values.
        if (hardScore == -1) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0) {
            return ZERO;
        } else if (hardScore == 1) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftScore(hardScore, 0);
    }

    public static @NonNull HardSoftScore ofSoft(int softScore) {
        // Optimization for frequently seen values.
        if (softScore == -1) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0) {
            return ZERO;
        } else if (softScore == 1) {
            return ONE_SOFT;
        }
        // Every other case is constructed.
        return new HardSoftScore(0, softScore);
    }

    private final int hardScore;
    private final int softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardSoftScore() {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private HardSoftScore(int hardScore, int softScore) {
        this.hardScore = hardScore;
        this.softScore = softScore;
    }

    /**
     * The total of the broken negative hard constraints and fulfilled positive hard constraints.
     * Their weight is included in the total.
     * The hard score is usually a negative number because most use cases only have negative constraints.
     *
     * @return higher is better, usually negative, 0 if no hard constraints are broken/fulfilled
     */
    public int hardScore() {
        return hardScore;
    }

    /**
     * As defined by {@link #hardScore()}.
     *
     * @deprecated Use {@link #hardScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public int getHardScore() {
        return hardScore;
    }

    /**
     * The total of the broken negative soft constraints and fulfilled positive soft constraints.
     * Their weight is included in the total.
     * The soft score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the soft score is irrelevant if the 2 scores don't have the same hard score.
     *
     * @return higher is better, usually negative, 0 if no soft constraints are broken/fulfilled
     */
    public int softScore() {
        return softScore;
    }

    /**
     * As defined by {@link #softScore()}.
     *
     * @deprecated Use {@link #softScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public int getSoftScore() {
        return softScore;
    }

    @Override
    public boolean isFeasible() {
        return hardScore >= 0;
    }

    @Override
    public @NonNull HardSoftScore add(@NonNull HardSoftScore addend) {
        return of(hardScore + addend.hardScore(),
                softScore + addend.softScore());
    }

    @Override
    public @NonNull HardSoftScore subtract(@NonNull HardSoftScore subtrahend) {
        return of(hardScore - subtrahend.hardScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public @NonNull HardSoftScore multiply(double multiplicand) {
        return of((int) Math.floor(hardScore * multiplicand),
                (int) Math.floor(softScore * multiplicand));
    }

    @Override
    public @NonNull HardSoftScore divide(double divisor) {
        return of((int) Math.floor(hardScore / divisor),
                (int) Math.floor(softScore / divisor));
    }

    @Override
    public @NonNull HardSoftScore power(double exponent) {
        return of((int) Math.floor(Math.pow(hardScore, exponent)),
                (int) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public @NonNull HardSoftScore abs() {
        return of(Math.abs(hardScore), Math.abs(softScore));
    }

    @Override
    public @NonNull HardSoftScore zero() {
        return ZERO;
    }

    @Override
    public Number @NonNull [] toLevelNumbers() {
        return new Number[] { hardScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardSoftScore other) {
            return hardScore == other.hardScore()
                    && softScore == other.softScore();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hardScore, softScore);
    }

    @Override
    public int compareTo(@NonNull HardSoftScore other) {
        if (hardScore != other.hardScore()) {
            return Integer.compare(hardScore, other.hardScore());
        } else {
            return Integer.compare(softScore, other.softScore());
        }
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.intValue() != 0, HARD_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return hardScore + HARD_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
