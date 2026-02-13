package ai.timefold.solver.core.api.score;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.MEDIUM_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

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
@NullMarked
public record HardMediumSoftScore(long hardScore, long mediumScore,
        long softScore) implements Score<HardMediumSoftScore> {

    public static final HardMediumSoftScore ZERO = new HardMediumSoftScore(0L, 0L, 0L);
    public static final HardMediumSoftScore ONE_HARD = new HardMediumSoftScore(1L, 0L, 0L);
    private static final HardMediumSoftScore MINUS_ONE_HARD = new HardMediumSoftScore(-1L, 0L, 0L);
    public static final HardMediumSoftScore ONE_MEDIUM = new HardMediumSoftScore(0L, 1L, 0L);
    private static final HardMediumSoftScore MINUS_ONE_MEDIUM = new HardMediumSoftScore(0L, -1L, 0L);
    public static final HardMediumSoftScore ONE_SOFT = new HardMediumSoftScore(0L, 0L, 1L);
    private static final HardMediumSoftScore MINUS_ONE_SOFT = new HardMediumSoftScore(0L, 0L, -1L);

    public static HardMediumSoftScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(HardMediumSoftScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        var hardScore = ScoreUtil.parseLevelAsLong(HardMediumSoftScore.class, scoreString, scoreTokens[0]);
        var mediumScore = ScoreUtil.parseLevelAsLong(HardMediumSoftScore.class, scoreString, scoreTokens[1]);
        var softScore = ScoreUtil.parseLevelAsLong(HardMediumSoftScore.class, scoreString, scoreTokens[2]);
        return of(hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftScore of(long hardScore, long mediumScore, long softScore) {
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
        return new HardMediumSoftScore(hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftScore ofHard(long hardScore) {
        if (hardScore == -1L) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0L) {
            return ZERO;
        } else if (hardScore == 1L) {
            return ONE_HARD;
        }
        return new HardMediumSoftScore(hardScore, 0L, 0L);
    }

    public static HardMediumSoftScore ofMedium(long mediumScore) {
        if (mediumScore == -1L) {
            return MINUS_ONE_MEDIUM;
        } else if (mediumScore == 0L) {
            return ZERO;
        } else if (mediumScore == 1L) {
            return ONE_MEDIUM;
        }
        return new HardMediumSoftScore(0L, mediumScore, 0L);
    }

    public static HardMediumSoftScore ofSoft(long softScore) {
        if (softScore == -1L) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0L) {
            return ZERO;
        } else if (softScore == 1L) {
            return ONE_SOFT;
        }
        return new HardMediumSoftScore(0L, 0L, softScore);
    }

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #hardScore()} is 0 or higher
     */
    @Override
    public boolean isFeasible() {
        return hardScore >= 0L;
    }

    @Override
    public HardMediumSoftScore add(HardMediumSoftScore addend) {
        return of(hardScore + addend.hardScore(),
                mediumScore + addend.mediumScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardMediumSoftScore subtract(HardMediumSoftScore subtrahend) {
        return of(hardScore - subtrahend.hardScore(),
                mediumScore - subtrahend.mediumScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardMediumSoftScore multiply(double multiplicand) {
        return of((long) Math.floor(hardScore * multiplicand),
                (long) Math.floor(mediumScore * multiplicand),
                (long) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardMediumSoftScore divide(double divisor) {
        return of((long) Math.floor(hardScore / divisor),
                (long) Math.floor(mediumScore / divisor),
                (long) Math.floor(softScore / divisor));
    }

    @Override
    public HardMediumSoftScore power(double exponent) {
        return of((long) Math.floor(Math.pow(hardScore, exponent)),
                (long) Math.floor(Math.pow(mediumScore, exponent)),
                (long) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardMediumSoftScore abs() {
        return of(Math.abs(hardScore), Math.abs(mediumScore), Math.abs(softScore));
    }

    @Override
    public HardMediumSoftScore zero() {
        return HardMediumSoftScore.ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, mediumScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardMediumSoftScore(var otherHardScore, var otherMediumScore, var otherSoftScore)) {
            return hardScore == otherHardScore
                    && mediumScore == otherMediumScore
                    && softScore == otherSoftScore;
        }
        return false;
    }

    @Override
    public int compareTo(HardMediumSoftScore other) {
        if (hardScore != other.hardScore()) {
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
        return hardScore + HARD_LABEL + "/" + mediumScore + MEDIUM_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
