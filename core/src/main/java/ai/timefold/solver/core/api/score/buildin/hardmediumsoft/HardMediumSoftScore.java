package ai.timefold.solver.core.api.score.buildin.hardmediumsoft;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.MEDIUM_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on 3 levels of int constraints: hard, medium and soft.
 * Hard constraints have priority over medium constraints.
 * Medium constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
@NullMarked
public final class HardMediumSoftScore implements Score<HardMediumSoftScore> {

    public static final HardMediumSoftScore ZERO = new HardMediumSoftScore(0, 0, 0);
    public static final HardMediumSoftScore ONE_HARD = new HardMediumSoftScore(1, 0, 0);
    private static final HardMediumSoftScore MINUS_ONE_HARD = new HardMediumSoftScore(-1, 0, 0);
    public static final HardMediumSoftScore ONE_MEDIUM = new HardMediumSoftScore(0, 1, 0);
    private static final HardMediumSoftScore MINUS_ONE_MEDIUM = new HardMediumSoftScore(0, -1, 0);
    public static final HardMediumSoftScore ONE_SOFT = new HardMediumSoftScore(0, 0, 1);
    private static final HardMediumSoftScore MINUS_ONE_SOFT = new HardMediumSoftScore(0, 0, -1);

    public static HardMediumSoftScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(HardMediumSoftScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        var hardScore = ScoreUtil.parseLevelAsInt(HardMediumSoftScore.class, scoreString, scoreTokens[0]);
        var mediumScore = ScoreUtil.parseLevelAsInt(HardMediumSoftScore.class, scoreString, scoreTokens[1]);
        var softScore = ScoreUtil.parseLevelAsInt(HardMediumSoftScore.class, scoreString, scoreTokens[2]);
        return of(hardScore, mediumScore, softScore);
    }

    /**
     * @deprecated Use {@link #of(int, int, int)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static HardMediumSoftScore ofUninitialized(int initScore, int hardScore, int mediumScore, int softScore) {
        return of(hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftScore of(int hardScore, int mediumScore, int softScore) {
        if (hardScore == -1 && mediumScore == 0 && softScore == 0) {
            return MINUS_ONE_HARD;
        } else if (hardScore == 0) {
            if (mediumScore == -1 && softScore == 0) {
                return MINUS_ONE_MEDIUM;
            } else if (mediumScore == 0) {
                if (softScore == -1) {
                    return MINUS_ONE_SOFT;
                } else if (softScore == 0) {
                    return ZERO;
                } else if (softScore == 1) {
                    return ONE_SOFT;
                }
            } else if (mediumScore == 1 && softScore == 0) {
                return ONE_MEDIUM;
            }
        } else if (hardScore == 1 && mediumScore == 0 && softScore == 0) {
            return ONE_HARD;
        }
        return new HardMediumSoftScore(hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftScore ofHard(int hardScore) {
        return switch (hardScore) {
            case -1 -> MINUS_ONE_HARD;
            case 0 -> ZERO;
            case 1 -> ONE_HARD;
            default -> new HardMediumSoftScore(hardScore, 0, 0);
        };
    }

    public static HardMediumSoftScore ofMedium(int mediumScore) {
        return switch (mediumScore) {
            case -1 -> MINUS_ONE_MEDIUM;
            case 0 -> ZERO;
            case 1 -> ONE_MEDIUM;
            default -> new HardMediumSoftScore(0, mediumScore, 0);
        };
    }

    public static HardMediumSoftScore ofSoft(int softScore) {
        return switch (softScore) {
            case -1 -> MINUS_ONE_SOFT;
            case 0 -> ZERO;
            case 1 -> ONE_SOFT;
            default -> new HardMediumSoftScore(0, 0, softScore);
        };
    }

    private final int hardScore;
    private final int mediumScore;
    private final int softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardMediumSoftScore() {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private HardMediumSoftScore(int hardScore, int mediumScore, int softScore) {
        this.hardScore = hardScore;
        this.mediumScore = mediumScore;
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
     * The total of the broken negative medium constraints and fulfilled positive medium constraints.
     * Their weight is included in the total.
     * The medium score is usually a negative number because most use cases only have negative constraints.
     * <p>
     * In a normal score comparison, the medium score is irrelevant if the 2 scores don't have the same hard score.
     *
     * @return higher is better, usually negative, 0 if no medium constraints are broken/fulfilled
     */
    public int mediumScore() {
        return mediumScore;
    }

    /**
     * As defined by {@link #mediumScore()}.
     *
     * @deprecated Use {@link #mediumScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public int getMediumScore() {
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

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #hardScore()} is 0 or higher
     */
    @Override
    public boolean isFeasible() {
        return hardScore >= 0;
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
        return of((int) Math.floor(hardScore * multiplicand),
                (int) Math.floor(mediumScore * multiplicand),
                (int) Math.floor(softScore * multiplicand));
    }

    @Override
    public HardMediumSoftScore divide(double divisor) {
        return of((int) Math.floor(hardScore / divisor),
                (int) Math.floor(mediumScore / divisor),
                (int) Math.floor(softScore / divisor));
    }

    @Override
    public HardMediumSoftScore power(double exponent) {
        return of((int) Math.floor(Math.pow(hardScore, exponent)),
                (int) Math.floor(Math.pow(mediumScore, exponent)),
                (int) Math.floor(Math.pow(softScore, exponent)));
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
        if (o instanceof HardMediumSoftScore other) {
            return hardScore == other.hardScore()
                    && mediumScore == other.mediumScore()
                    && softScore == other.softScore();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hardScore, mediumScore, softScore);
    }

    @Override
    public int compareTo(HardMediumSoftScore other) {
        if (hardScore != other.hardScore()) {
            return Integer.compare(hardScore, other.hardScore());
        } else if (mediumScore != other.mediumScore()) {
            return Integer.compare(mediumScore, other.mediumScore());
        } else {
            return Integer.compare(softScore, other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.intValue() != 0, HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return hardScore + HARD_LABEL + "/" + mediumScore + MEDIUM_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
