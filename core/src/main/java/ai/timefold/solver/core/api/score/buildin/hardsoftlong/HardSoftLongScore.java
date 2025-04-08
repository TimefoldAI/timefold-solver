package ai.timefold.solver.core.api.score.buildin.hardsoftlong;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on 2 levels of long constraints: hard and soft.
 * Hard constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
@NullMarked
public final class HardSoftLongScore implements Score<HardSoftLongScore> {

    public static final HardSoftLongScore ZERO = new HardSoftLongScore(0L, 0L);
    public static final HardSoftLongScore ONE_HARD = new HardSoftLongScore(1L, 0L);
    public static final HardSoftLongScore ONE_SOFT = new HardSoftLongScore(0L, 1L);
    private static final HardSoftLongScore MINUS_ONE_SOFT = new HardSoftLongScore(0L, -1L);
    private static final HardSoftLongScore MINUS_ONE_HARD = new HardSoftLongScore(-1L, 0L);

    public static HardSoftLongScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(HardSoftLongScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        var hardScore = ScoreUtil.parseLevelAsLong(HardSoftLongScore.class, scoreString, scoreTokens[0]);
        var softScore = ScoreUtil.parseLevelAsLong(HardSoftLongScore.class, scoreString, scoreTokens[1]);
        return of(hardScore, softScore);
    }

    /**
     * @deprecated Use {@link #of(long, long)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static HardSoftLongScore ofUninitialized(int initScore, long hardScore, long softScore) {
        return of(hardScore, softScore);
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
        return new HardSoftLongScore(hardScore, softScore);
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
        return new HardSoftLongScore(hardScore, 0L);
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
        return new HardSoftLongScore(0L, softScore);
    }

    private final long hardScore;
    private final long softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardSoftLongScore() {
        this(Long.MIN_VALUE, Long.MIN_VALUE);
    }

    private HardSoftLongScore(long hardScore, long softScore) {
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

    @Override
    public boolean isFeasible() {
        return hardScore >= 0L;
    }

    @Override
    public HardSoftLongScore add(HardSoftLongScore addend) {
        return of(hardScore + addend.hardScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardSoftLongScore subtract(HardSoftLongScore subtrahend) {
        return of(hardScore - subtrahend.hardScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardSoftLongScore multiply(double multiplicand) {
        return of((long) Math.floor(hardScore * multiplicand),
                (long) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardSoftLongScore divide(double divisor) {
        return of((long) Math.floor(hardScore / divisor),
                (long) Math.floor(softScore / divisor));
    }

    @Override
    public HardSoftLongScore power(double exponent) {
        return of((long) Math.floor(Math.pow(hardScore, exponent)),
                (long) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardSoftLongScore abs() {
        return of(Math.abs(hardScore), Math.abs(softScore));
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
    public int compareTo(HardSoftLongScore other) {
        if (hardScore != other.hardScore()) {
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
        return hardScore + HARD_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
