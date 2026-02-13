package ai.timefold.solver.core.api.score;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

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
public record HardSoftScore(long hardScore, long softScore) implements Score<HardSoftScore> {

    public static final HardSoftScore ZERO = new HardSoftScore(0L, 0L);
    public static final HardSoftScore ONE_HARD = new HardSoftScore(1L, 0L);
    public static final HardSoftScore ONE_SOFT = new HardSoftScore(0L, 1L);
    private static final HardSoftScore MINUS_ONE_SOFT = new HardSoftScore(0L, -1L);
    private static final HardSoftScore MINUS_ONE_HARD = new HardSoftScore(-1L, 0L);

    public static HardSoftScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(HardSoftScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        var hardScore = ScoreUtil.parseLevelAsLong(HardSoftScore.class, scoreString, scoreTokens[0]);
        var softScore = ScoreUtil.parseLevelAsLong(HardSoftScore.class, scoreString, scoreTokens[1]);
        return of(hardScore, softScore);
    }

    public static HardSoftScore of(long hardScore, long softScore) {
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
        return new HardSoftScore(hardScore, softScore);
    }

    public static HardSoftScore ofHard(long hardScore) {
        // Optimization for frequently seen values.
        if (hardScore == -1L) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0L) {
            return ZERO;
        } else if (hardScore == 1L) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftScore(hardScore, 0L);
    }

    public static HardSoftScore ofSoft(long softScore) {
        // Optimization for frequently seen values.
        if (softScore == -1L) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0L) {
            return ZERO;
        } else if (softScore == 1L) {
            return ONE_SOFT;
        }
        // Every other case is constructed.
        return new HardSoftScore(0L, softScore);
    }

    @Override
    public boolean isFeasible() {
        return hardScore >= 0L;
    }

    @Override
    public HardSoftScore add(HardSoftScore addend) {
        return of(hardScore + addend.hardScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardSoftScore subtract(HardSoftScore subtrahend) {
        return of(hardScore - subtrahend.hardScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardSoftScore multiply(double multiplicand) {
        return of((long) Math.floor(hardScore * multiplicand),
                (long) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardSoftScore divide(double divisor) {
        return of((long) Math.floor(hardScore / divisor),
                (long) Math.floor(softScore / divisor));
    }

    @Override
    public HardSoftScore power(double exponent) {
        return of((long) Math.floor(Math.pow(hardScore, exponent)),
                (long) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardSoftScore abs() {
        return of(Math.abs(hardScore), Math.abs(softScore));
    }

    @Override
    public HardSoftScore zero() {
        return ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardSoftScore(var otherHardScore, var otherSoftScore)) {
            return hardScore == otherHardScore
                    && softScore == otherSoftScore;
        }
        return false;
    }

    @Override
    public int compareTo(HardSoftScore other) {
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
