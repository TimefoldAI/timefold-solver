package ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.MEDIUM_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 3 levels of {@link BigDecimal} constraints: hard, medium and soft.
 * Hard constraints have priority over medium constraints.
 * Medium constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class HardMediumSoftBigDecimalScore implements Score<HardMediumSoftBigDecimalScore> {

    public static final HardMediumSoftBigDecimalScore ZERO = new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_HARD = new HardMediumSoftBigDecimalScore(0, BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO);
    private static final HardMediumSoftBigDecimalScore MINUS_ONE_HARD =
            new HardMediumSoftBigDecimalScore(0, BigDecimal.ONE.negate(),
                    BigDecimal.ZERO, BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_MEDIUM = new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO,
            BigDecimal.ONE, BigDecimal.ZERO);
    private static final HardMediumSoftBigDecimalScore MINUS_ONE_MEDIUM = new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO,
            BigDecimal.ONE.negate(), BigDecimal.ZERO);
    public static final HardMediumSoftBigDecimalScore ONE_SOFT = new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ONE);
    private static final HardMediumSoftBigDecimalScore MINUS_ONE_SOFT =
            new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE.negate());

    public static HardMediumSoftBigDecimalScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(HardMediumSoftBigDecimalScore.class, scoreString,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
        int initScore = ScoreUtil.parseInitScore(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[0]);
        BigDecimal hardScore =
                ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[1]);
        BigDecimal mediumScore =
                ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[2]);
        BigDecimal softScore =
                ScoreUtil.parseLevelAsBigDecimal(HardMediumSoftBigDecimalScore.class, scoreString, scoreTokens[3]);
        return ofUninitialized(initScore, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftBigDecimalScore ofUninitialized(int initScore, BigDecimal hardScore, BigDecimal mediumScore,
            BigDecimal softScore) {
        if (initScore == 0) {
            return of(hardScore, mediumScore, softScore);
        }
        return new HardMediumSoftBigDecimalScore(initScore, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftBigDecimalScore of(BigDecimal hardScore, BigDecimal mediumScore, BigDecimal softScore) {
        if (Objects.equals(hardScore, BigDecimal.ONE.negate()) && mediumScore.signum() == 0 && softScore.signum() == 0) {
            return MINUS_ONE_HARD;
        } else if (hardScore.signum() == 0) {
            if (Objects.equals(mediumScore, BigDecimal.ONE.negate()) && softScore.signum() == 0) {
                return MINUS_ONE_MEDIUM;
            } else if (mediumScore.signum() == 0) {
                if (Objects.equals(softScore, BigDecimal.ONE.negate())) {
                    return MINUS_ONE_SOFT;
                } else if (softScore.signum() == 0) {
                    return ZERO;
                } else if (Objects.equals(softScore, BigDecimal.ONE)) {
                    return ONE_SOFT;
                }
            } else if (Objects.equals(mediumScore, BigDecimal.ONE) && softScore.signum() == 0) {
                return ONE_MEDIUM;
            }
        } else if (Objects.equals(hardScore, BigDecimal.ONE) && mediumScore.signum() == 0 && softScore.signum() == 0) {
            return ONE_HARD;
        }
        return new HardMediumSoftBigDecimalScore(0, hardScore, mediumScore, softScore);
    }

    public static HardMediumSoftBigDecimalScore ofHard(BigDecimal hardScore) {
        if (Objects.equals(hardScore, BigDecimal.ONE.negate())) {
            return MINUS_ONE_HARD;
        } else if (hardScore.signum() == 0) {
            return ZERO;
        } else if (Objects.equals(hardScore, BigDecimal.ONE)) {
            return ONE_HARD;
        }
        return new HardMediumSoftBigDecimalScore(0, hardScore, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static HardMediumSoftBigDecimalScore ofMedium(BigDecimal mediumScore) {
        if (Objects.equals(mediumScore, BigDecimal.ONE.negate())) {
            return MINUS_ONE_MEDIUM;
        } else if (mediumScore.signum() == 0) {
            return ZERO;
        } else if (Objects.equals(mediumScore, BigDecimal.ONE)) {
            return ONE_MEDIUM;
        }
        return new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO, mediumScore, BigDecimal.ZERO);
    }

    public static HardMediumSoftBigDecimalScore ofSoft(BigDecimal softScore) {
        if (Objects.equals(softScore, BigDecimal.ONE.negate())) {
            return MINUS_ONE_SOFT;
        } else if (softScore.signum() == 0) {
            return ZERO;
        } else if (Objects.equals(softScore, BigDecimal.ONE)) {
            return ONE_SOFT;
        }
        return new HardMediumSoftBigDecimalScore(0, BigDecimal.ZERO, BigDecimal.ZERO, softScore);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final BigDecimal hardScore;
    private final BigDecimal mediumScore;
    private final BigDecimal softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardMediumSoftBigDecimalScore() {
        this(Integer.MIN_VALUE, null, null, null);
    }

    private HardMediumSoftBigDecimalScore(int initScore, BigDecimal hardScore, BigDecimal mediumScore, BigDecimal softScore) {
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
    public BigDecimal hardScore() {
        return hardScore;
    }

    /**
     * As defined by {@link #hardScore()}.
     *
     * @deprecated Use {@link #hardScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getHardScore() {
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
    public BigDecimal mediumScore() {
        return mediumScore;
    }

    /**
     * As defined by {@link #mediumScore()}.
     *
     * @deprecated Use {@link #mediumScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getMediumScore() {
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
    public BigDecimal softScore() {
        return softScore;
    }

    /**
     * As defined by {@link #softScore()}.
     *
     * @deprecated Use {@link #softScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getSoftScore() {
        return softScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public HardMediumSoftBigDecimalScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, hardScore, mediumScore, softScore);
    }

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     *
     * @return true if the {@link #hardScore()} is 0 or higher
     */
    @Override
    public boolean isFeasible() {
        return initScore >= 0 && hardScore.compareTo(BigDecimal.ZERO) >= 0;
    }

    @Override
    public HardMediumSoftBigDecimalScore add(HardMediumSoftBigDecimalScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                hardScore.add(addend.hardScore()),
                mediumScore.add(addend.mediumScore()),
                softScore.add(addend.softScore()));
    }

    @Override
    public HardMediumSoftBigDecimalScore subtract(HardMediumSoftBigDecimalScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                hardScore.subtract(subtrahend.hardScore()),
                mediumScore.subtract(subtrahend.mediumScore()),
                softScore.subtract(subtrahend.softScore()));
    }

    @Override
    public HardMediumSoftBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                hardScore.multiply(multiplicandBigDecimal).setScale(hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.multiply(multiplicandBigDecimal).setScale(mediumScore.scale(), RoundingMode.FLOOR),
                softScore.multiply(multiplicandBigDecimal).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore divide(double divisor) {
        BigDecimal divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                hardScore.divide(divisorBigDecimal, hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.divide(divisorBigDecimal, mediumScore.scale(), RoundingMode.FLOOR),
                softScore.divide(divisorBigDecimal, softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore power(double exponent) {
        BigDecimal exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                hardScore.pow(exponentBigDecimal.intValue()).setScale(hardScore.scale(), RoundingMode.FLOOR),
                mediumScore.pow(exponentBigDecimal.intValue()).setScale(mediumScore.scale(), RoundingMode.FLOOR),
                softScore.pow(exponentBigDecimal.intValue()).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public HardMediumSoftBigDecimalScore abs() {
        return ofUninitialized(Math.abs(initScore), hardScore.abs(), mediumScore.abs(), softScore.abs());
    }

    @Override
    public HardMediumSoftBigDecimalScore zero() {
        return HardMediumSoftBigDecimalScore.ZERO;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { hardScore, mediumScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardMediumSoftBigDecimalScore other) {
            return initScore == other.initScore()
                    && hardScore.stripTrailingZeros().equals(other.hardScore().stripTrailingZeros())
                    && mediumScore.stripTrailingZeros().equals(other.mediumScore().stripTrailingZeros())
                    && softScore.stripTrailingZeros().equals(other.softScore().stripTrailingZeros());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, hardScore.stripTrailingZeros(), mediumScore.stripTrailingZeros(),
                softScore.stripTrailingZeros());
    }

    @Override
    public int compareTo(HardMediumSoftBigDecimalScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        }
        int hardScoreComparison = hardScore.compareTo(other.hardScore());
        if (hardScoreComparison != 0) {
            return hardScoreComparison;
        }
        int mediumScoreComparison = mediumScore.compareTo(other.mediumScore());
        if (mediumScoreComparison != 0) {
            return mediumScoreComparison;
        } else {
            return softScore.compareTo(other.softScore());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0,
                HARD_LABEL, MEDIUM_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + hardScore + HARD_LABEL + "/" + mediumScore + MEDIUM_LABEL + "/" + softScore
                + SOFT_LABEL;
    }

}
