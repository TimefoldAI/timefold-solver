package ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.MEDIUM_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 3 levels of long constraints: hard, medium and soft.
 * Hard constraints have priority over medium constraints.
 * Medium constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class HardMediumSoftLongScore implements Score<HardMediumSoftLongScore> {

    public static final HardMediumSoftLongScore ZERO = new HardMediumSoftLongScore(0, 0L, 0L, 0L);
    public static final HardMediumSoftLongScore ONE_HARD = new HardMediumSoftLongScore(0, 1L, 0L, 0L);
    private static final HardMediumSoftLongScore MINUS_ONE_HARD = new HardMediumSoftLongScore(0, -1L, 0L, 0L);
    public static final HardMediumSoftLongScore ONE_MEDIUM = new HardMediumSoftLongScore(0, 0L, 1L, 0L);
    private static final HardMediumSoftLongScore MINUS_ONE_MEDIUM = new HardMediumSoftLongScore(0, 0L, -1L, 0L);
    public static final HardMediumSoftLongScore ONE_SOFT = new HardMediumSoftLongScore(0, 0L, 0L, 1L);
    private static final HardMediumSoftLongScore MINUS_ONE_SOFT = new HardMediumSoftLongScore(0, 0L, 0L, -1L);

    public static HardMediumSoftLongScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(HardMediumSoftLongScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        int initScore = ScoreUtil.parseInitScore(HardMediumSoftLongScore.class, scoreString, scoreTokens[0]);
        long hardScore = ScoreUtil.parseLevelAsLong(HardMediumSoftLongScore.class, scoreString, scoreTokens[1]);
        long mediumScore = ScoreUtil.parseLevelAsLong(HardMediumSoftLongScore.class, scoreString, scoreTokens[2]);
        long softScore = ScoreUtil.parseLevelAsLong(HardMediumSoftLongScore.class, scoreString, scoreTokens[3]);
        return ofUninitialized(initScore, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftLongScore ofUninitialized(int initScore, long hardScore, long mediumScore, long softScore) {
        if (initScore == 0) {
            return of(hardScore, mediumScore, softScore);
        }
        return new HardMediumSoftLongScore(initScore, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftLongScore of(long hardScore, long mediumScore, long softScore) {
        if (hardScore == -1L && mediumScore == 0L && softScore == 0L) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0L) {
            if (mediumScore == -1L && softScore == 0L) {
                return MINUS_ONE_MEDIUM;
            } else if (mediumScore == 0L) {
                if (softScore == -1L) {
                    return MINUS_ONE_SOFT;
                } else if (softScore == 0L) {
                    return ZERO;
                } else if (softScore == 1L) {
                    return ONE_SOFT;
                }
            } else if (mediumScore == 1L && softScore == 0L) {
                return ONE_MEDIUM;
            }
        } else if (hardScore == 1L && mediumScore == 0L && softScore == 0L) {
            return ONE_HARD;
        }
        return new HardMediumSoftLongScore(0, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftLongScore ofHard(long hardScore) {
        if (hardScore == -1L) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0L) {
            return ZERO;
        } else if (hardScore == 1L) {
            return ONE_HARD;
        }
        return new HardMediumSoftLongScore(0, hardScore, 0L, 0L);
    }

    public static HardMediumSoftLongScore ofMedium(long mediumScore) {
        if (mediumScore == -1L) {
            return MINUS_ONE_MEDIUM;
        } else if (mediumScore == 0L) {
            return ZERO;
        } else if (mediumScore == 1L) {
            return ONE_MEDIUM;
        }
        return new HardMediumSoftLongScore(0, 0L, mediumScore, 0L);
    }

    public static HardMediumSoftLongScore ofSoft(long softScore) {
        if (softScore == -1L) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0L) {
            return ZERO;
        } else if (softScore == 1L) {
            return ONE_SOFT;
        }
        return new HardMediumSoftLongScore(0, 0L, 0L, softScore);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final long hardScore;
    private final long mediumScore;
    private final long softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardMediumSoftLongScore() {
        this(Integer.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private HardMediumSoftLongScore(int initScore, long hardScore, long mediumScore, long softScore) {
        this.initScore = initScore;
        this.hardScore = hardScore;
        this.mediumScore = mediumScore;
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
     * The total of the broken negative medium constraints and fulfilled positive medium constraints.
     * Their weight is included in the total.
     * The medium score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the medium score is irrelevant if the 2 scores don't have the same hard score.
     *
     * @return higher is better, usually negative, 0 if no medium constraints are broken/fulfilled
     */
    public long mediumScore() {
        return mediumScore;
    }

    /**
     * As defined by {@link #mediumScore()}.
     *
     * @deprecated Use {@link #mediumScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public long getMediumScore() {
        return mediumScore;
    }

    /**
     * The total of the broken negative soft constraints and fulfilled positive soft constraints.
     * Their weight is included in the total.
     * The soft score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the soft score is irrelevant if the 2 scores don't have the same hard and medium score.
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
    public HardMediumSoftLongScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, hardScore, mediumScore, softScore);
    }

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #hardScore()} is 0 or higher
     */
    @Override
    public boolean isFeasible() {
        return initScore >= 0 && hardScore >= 0L;
    }

    @Override
    public HardMediumSoftLongScore add(HardMediumSoftLongScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                hardScore + addend.hardScore(),
                mediumScore + addend.mediumScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardMediumSoftLongScore subtract(HardMediumSoftLongScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                hardScore - subtrahend.hardScore(),
                mediumScore - subtrahend.mediumScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardMediumSoftLongScore multiply(double multiplicand) {
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                (long) Math.floor(hardScore * multiplicand),
                (long) Math.floor(mediumScore * multiplicand),
                (long) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardMediumSoftLongScore divide(double divisor) {
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                (long) Math.floor(hardScore / divisor),
                (long) Math.floor(mediumScore / divisor),
                (long) Math.floor(softScore / divisor));
    }

    @Override
    public HardMediumSoftLongScore power(double exponent) {
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                (long) Math.floor(Math.pow(hardScore, exponent)),
                (long) Math.floor(Math.pow(mediumScore, exponent)),
                (long) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardMediumSoftLongScore abs() {
        return ofUninitialized(Math.abs(initScore), Math.abs(hardScore), Math.abs(mediumScore), Math.abs(softScore));
    }

    @Override
    public HardMediumSoftLongScore zero() {
        return HardMediumSoftLongScore.ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, mediumScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardMediumSoftLongScore other) {
            return initScore == other.initScore()
                    && hardScore == other.hardScore()
                    && mediumScore == other.mediumScore()
                    && softScore == other.softScore();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, hardScore, mediumScore, softScore);
    }

    @Override
    public int compareTo(HardMediumSoftLongScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else if (hardScore != other.hardScore()) {
            return Long.compare(hardScore, other.hardScore());
        } else if (mediumScore != other.mediumScore()) {
            return Long.compare(mediumScore, other.mediumScore());
        } else {
            return Long.compare(softScore, other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.longValue() != 0L, HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + hardScore + HARD_LABEL + "/" + mediumScore + MEDIUM_LABEL + "/" + softScore
                + SOFT_LABEL;
    }

}
