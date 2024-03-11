package ai.timefold.solver.core.api.score.buildin.hardsoft;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.parseInitScore;
import static ai.timefold.solver.core.impl.score.ScoreUtil.parseLevelAsInt;
import static ai.timefold.solver.core.impl.score.ScoreUtil.parseScoreTokens;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

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

    public static final HardSoftScore ZERO = new HardSoftScore(0, 0, 0);
    public static final HardSoftScore ONE_HARD = new HardSoftScore(0, 1, 0);
    public static final HardSoftScore ONE_SOFT = new HardSoftScore(0, 0, 1);
    private static final HardSoftScore MINUS_ONE_SOFT = new HardSoftScore(0, 0, -1);
    private static final HardSoftScore MINUS_ONE_HARD = new HardSoftScore(0, -1, 0);

    public static HardSoftScore parseScore(String scoreString) {
        String[] scoreTokens = parseScoreTokens(HardSoftScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        int initScore = parseInitScore(HardSoftScore.class, scoreString, scoreTokens[0]);
        int hardScore = parseLevelAsInt(HardSoftScore.class, scoreString, scoreTokens[1]);
        int softScore = parseLevelAsInt(HardSoftScore.class, scoreString, scoreTokens[2]);
        return ofUninitialized(initScore, hardScore, softScore);
    }

    public static HardSoftScore ofUninitialized(int initScore, int hardScore, int softScore) {
        if (initScore == 0) {
            return of(hardScore, softScore);
        }
        return new HardSoftScore(initScore, hardScore, softScore);
    }

    public static HardSoftScore of(int hardScore, int softScore) {
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
        return new HardSoftScore(0, hardScore, softScore);
    }

    public static HardSoftScore ofHard(int hardScore) {
        // Optimization for frequently seen values.
        if (hardScore == -1) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0) {
            return ZERO;
        } else if (hardScore == 1) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftScore(0, hardScore, 0);
    }

    public static HardSoftScore ofSoft(int softScore) {
        // Optimization for frequently seen values.
        if (softScore == -1) {
            return MINUS_ONE_SOFT;
        } else if (softScore == 0) {
            return ZERO;
        } else if (softScore == 1) {
            return ONE_SOFT;
        }
        // Every other case is constructed.
        return new HardSoftScore(0, 0, softScore);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final int hardScore;
    private final int softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardSoftScore() {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private HardSoftScore(int initScore, int hardScore, int softScore) {
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

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public HardSoftScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, hardScore, softScore);
    }

    @Override
    public boolean isFeasible() {
        return initScore >= 0 && hardScore >= 0;
    }

    @Override
    public HardSoftScore add(HardSoftScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                hardScore + addend.hardScore(),
                softScore + addend.softScore());
    }

    @Override
    public HardSoftScore subtract(HardSoftScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                hardScore - subtrahend.hardScore(),
                softScore - subtrahend.softScore());
    }

    @Override
    public HardSoftScore multiply(double multiplicand) {
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                (int) Math.floor(hardScore * multiplicand),
                (int) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardSoftScore divide(double divisor) {
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                (int) Math.floor(hardScore / divisor),
                (int) Math.floor(softScore / divisor));
    }

    @Override
    public HardSoftScore power(double exponent) {
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                (int) Math.floor(Math.pow(hardScore, exponent)),
                (int) Math.floor(Math.pow(softScore, exponent)));
    }

    @Override
    public HardSoftScore abs() {
        return ofUninitialized(Math.abs(initScore), Math.abs(hardScore), Math.abs(softScore));
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
        if (o instanceof HardSoftScore other) {
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
    public int compareTo(HardSoftScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else if (hardScore != other.hardScore()) {
            return Integer.compare(hardScore, other.hardScore());
        } else {
            return Integer.compare(softScore, other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.intValue() != 0, HARD_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + hardScore + HARD_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
