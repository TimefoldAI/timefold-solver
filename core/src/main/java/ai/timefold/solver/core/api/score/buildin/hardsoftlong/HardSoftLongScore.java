package ai.timefold.solver.core.api.score.buildin.hardsoftlong;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 2 levels of long constraints: hard and soft.
 * Hard constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class HardSoftLongScore implements Score<HardSoftLongScore> {

    public static final HardSoftLongScore ZERO = new HardSoftLongScore(0, 0L, 0L);
    public static final HardSoftLongScore ONE_HARD = new HardSoftLongScore(0, 1L, 0L);
    public static final HardSoftLongScore ONE_SOFT = new HardSoftLongScore(0, 0L, 1L);
    private static final HardSoftLongScore MINUS_ONE_SOFT = new HardSoftLongScore(0, 0L, -1L);
    private static final HardSoftLongScore MINUS_ONE_HARD = new HardSoftLongScore(0, -1L, 0L);

    public static HardSoftLongScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(HardSoftLongScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        int initScore = ScoreUtil.parseInitScore(HardSoftLongScore.class, scoreString, scoreTokens[0]);
        long hardScore = ScoreUtil.parseLevelAsLong(HardSoftLongScore.class, scoreString, scoreTokens[1]);
        long softScore = ScoreUtil.parseLevelAsLong(HardSoftLongScore.class, scoreString, scoreTokens[2]);
        return ofUninitialized(initScore, hardScore, softScore);
    }

    public static HardSoftLongScore ofUninitialized(int initScore, long hardScore, long softScore) {
        if (initScore == 0) {
            return of(hardScore, softScore);
        }
        return new HardSoftLongScore(initScore, hardScore, softScore);
    }

    public static HardSoftLongScore of(long hardScore, long softScore) {
        // Optimization for frequently seen values.
        if (hardScore == 0L) {
            if (softScore == -1L) {
                return MINUS_ONE_SOFT;
            } else if (softScore == 0L) {
                return ZERO;
            } else if (softScore == 1L) {
                return ONE_SOFT;
            }
        } else if (softScore == 0L) {
            if (hardScore == 1L) {
                return ONE_HARD;
            } else if (hardScore == -1L) {
                return MINUS_ONE_HARD;
            }
        }
        // Every other case is constructed.
        return new HardSoftLongScore(0, hardScore, softScore);
    }

    public static HardSoftLongScore ofHard(long hardScore) {
        // Optimization for frequently seen values.
        if (hardScore == -1L) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0L) {
            return ZERO;
        } else if (hardScore == 1L) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftLongScore(0, hardScore, 0L);
    }

    public static HardSoftLongScore ofSoft(long softScore) {
        // Optimization for frequently seen values.
        if (softScore == -1L) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0L) {
            return ZERO;
        } else if (softScore == 1L) {
            return ONE_SOFT;
        }
        // Every other case is constructed.
        return new HardSoftLongScore(0, 0L, softScore);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final long hardScore;
    private final long softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardSoftLongScore() {
        this(Integer.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private HardSoftLongScore(int initScore, long hardScore, long softScore) {
        this.initScore = initScore;
        this.hardScore = hardScore;
        this.softScore = softScore;
    }

    @Override
    public int initScore() {
        return initScore;
    }

    /**
     * The total of the broken negative hard constraints and fulfilled positive hard constraints.
     * Their weight is included in the total.
     * The hard score is usually a negative number because most use cases only have negative constraints.
     *
     * @return higher is better, usually negative, 0 if no hard constraints are broken/fulfilled
     */
    public long hardScore() {
        return hardScore;
    }

    /**
     * As defined by {@link #hardScore()}.
     *
     * @deprecated Use {@link #hardScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public long getHardScore() {
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
    public long softScore() {
        return softScore;
    }

    /**
     * As defined by {@link #softScore()}.
     *
     * @deprecated Use {@link #softScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public long getSoftScore() {
        return softScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public HardSoftLongScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, hardScore, softScore);
    }

    @Override
    public boolean isFeasible() {
        return initScore >= 0 && hardScore >= 0L;
    }

    @Override
    public HardSoftLongScore add(HardSoftLongScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                hardScore + addend.hardScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardSoftLongScore subtract(HardSoftLongScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                hardScore - subtrahend.hardScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardSoftLongScore multiply(double multiplicand) {
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                (long) Math.floor(hardScore * multiplicand),
                (long) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardSoftLongScore divide(double divisor) {
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                (long) Math.floor(hardScore / divisor),
                (long) Math.floor(softScore / divisor));
    }

    @Override
    public HardSoftLongScore power(double exponent) {
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                (long) Math.floor(Math.pow(hardScore, exponent)),
                (long) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardSoftLongScore abs() {
        return ofUninitialized(Math.abs(initScore), Math.abs(hardScore), Math.abs(softScore));
    }

    @Override
    public HardSoftLongScore zero() {
        return ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardSoftLongScore other) {
            return initScore == other.initScore()
                    && hardScore == other.hardScore()
                    && softScore == other.softScore();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, hardScore, softScore);
    }

    @Override
    public int compareTo(HardSoftLongScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else if (hardScore != other.hardScore()) {
            return Long.compare(hardScore, other.hardScore());
        } else {
            return Long.compare(softScore, other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.longValue() != 0L, HARD_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + hardScore + HARD_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
