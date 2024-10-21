package ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal;

import static ai.timefold.solver.core.impl.score.ScoreUtil.HARD_LABEL;
import static ai.timefold.solver.core.impl.score.ScoreUtil.SOFT_LABEL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on 2 levels of {@link BigDecimal} constraints: hard and soft.
 * Hard constraints have priority over soft constraints.
 * Hard constraints determine feasibility.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class HardSoftBigDecimalScore implements Score<HardSoftBigDecimalScore> {

    public static final @NonNull HardSoftBigDecimalScore ZERO =
            new HardSoftBigDecimalScore(0, BigDecimal.ZERO, BigDecimal.ZERO);
    public static final @NonNull HardSoftBigDecimalScore ONE_HARD =
            new HardSoftBigDecimalScore(0, BigDecimal.ONE, BigDecimal.ZERO);
    public static final @NonNull HardSoftBigDecimalScore ONE_SOFT =
            new HardSoftBigDecimalScore(0, BigDecimal.ZERO, BigDecimal.ONE);

    public static @NonNull HardSoftBigDecimalScore parseScore(@NonNull String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(HardSoftBigDecimalScore.class, scoreString, HARD_LABEL, SOFT_LABEL);
        int initScore = ScoreUtil.parseInitScore(HardSoftBigDecimalScore.class, scoreString, scoreTokens[0]);
        BigDecimal hardScore = ScoreUtil.parseLevelAsBigDecimal(HardSoftBigDecimalScore.class, scoreString, scoreTokens[1]);
        BigDecimal softScore = ScoreUtil.parseLevelAsBigDecimal(HardSoftBigDecimalScore.class, scoreString, scoreTokens[2]);
        return ofUninitialized(initScore, hardScore, softScore);
    }

    public static @NonNull HardSoftBigDecimalScore ofUninitialized(int initScore, @NonNull BigDecimal hardScore,
            @NonNull BigDecimal softScore) {
        if (initScore == 0) {
            return of(hardScore, softScore);
        }
        return new HardSoftBigDecimalScore(initScore, hardScore, softScore);
    }

    public static @NonNull HardSoftBigDecimalScore of(BigDecimal hardScore, BigDecimal softScore) {
        // Optimization for frequently seen values.
        if (hardScore.signum() == 0) {
            if (softScore.signum() == 0) {
                return ZERO;
            } else if (Objects.equals(softScore, BigDecimal.ONE)) {
                return ONE_SOFT;
            }
        } else if (Objects.equals(hardScore, BigDecimal.ONE) && softScore.signum() == 0) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftBigDecimalScore(0, hardScore, softScore);
    }

    public static @NonNull HardSoftBigDecimalScore ofHard(BigDecimal hardScore) {
        // Optimization for frequently seen values.
        if (hardScore.signum() == 0) {
            return ZERO;
        } else if (Objects.equals(hardScore, BigDecimal.ONE)) {
            return ONE_HARD;
        }
        // Every other case is constructed.
        return new HardSoftBigDecimalScore(0, hardScore, BigDecimal.ZERO);
    }

    public static @NonNull HardSoftBigDecimalScore ofSoft(BigDecimal softScore) {
        // Optimization for frequently seen values.
        if (softScore.signum() == 0) {
            return ZERO;
        } else if (Objects.equals(softScore, BigDecimal.ONE)) {
            return ONE_SOFT;
        }
        // Every other case is constructed.
        return new HardSoftBigDecimalScore(0, BigDecimal.ZERO, softScore);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final @NonNull BigDecimal hardScore;
    private final @NonNull BigDecimal softScore;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private HardSoftBigDecimalScore() {
        this(Integer.MIN_VALUE, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private HardSoftBigDecimalScore(int initScore, @NonNull BigDecimal hardScore, @NonNull BigDecimal softScore) {
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
    public @NonNull BigDecimal hardScore() {
        return hardScore;
    }

    /**
     * As defined by {@link #hardScore()}.
     *
     * @deprecated Use {@link #hardScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getHardScore() {
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
    public @NonNull BigDecimal softScore() {
        return softScore;
    }

    /**
     * As defined by {@link #softScore()}.
     *
     * @deprecated Use {@link #softScore()} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getSoftScore() {
        return softScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public @NonNull HardSoftBigDecimalScore withInitScore(int newInitScore) {
        return new HardSoftBigDecimalScore(newInitScore, hardScore, softScore);
    }

    @Override
    public boolean isFeasible() {
        return initScore >= 0 && hardScore.signum() >= 0;
    }

    @Override
    public @NonNull HardSoftBigDecimalScore add(@NonNull HardSoftBigDecimalScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                hardScore.add(addend.hardScore()),
                softScore.add(addend.softScore()));
    }

    @Override
    public @NonNull HardSoftBigDecimalScore subtract(@NonNull HardSoftBigDecimalScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                hardScore.subtract(subtrahend.hardScore()),
                softScore.subtract(subtrahend.softScore()));
    }

    @Override
    public @NonNull HardSoftBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                hardScore.multiply(multiplicandBigDecimal).setScale(hardScore.scale(), RoundingMode.FLOOR),
                softScore.multiply(multiplicandBigDecimal).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull HardSoftBigDecimalScore divide(double divisor) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                hardScore.divide(divisorBigDecimal, hardScore.scale(), RoundingMode.FLOOR),
                softScore.divide(divisorBigDecimal, softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull HardSoftBigDecimalScore power(double exponent) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                hardScore.pow(exponentBigDecimal.intValue()).setScale(hardScore.scale(), RoundingMode.FLOOR),
                softScore.pow(exponentBigDecimal.intValue()).setScale(softScore.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull HardSoftBigDecimalScore abs() {
        return ofUninitialized(Math.abs(initScore), hardScore.abs(), softScore.abs());
    }

    @Override
    public @NonNull HardSoftBigDecimalScore zero() {
        return ZERO;
    }

    @Override
    public Number @NonNull [] toLevelNumbers() {
        return new Number[] { hardScore, softScore };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HardSoftBigDecimalScore other) {
            return initScore == other.initScore()
                    && hardScore.stripTrailingZeros().equals(other.hardScore().stripTrailingZeros())
                    && softScore.stripTrailingZeros().equals(other.softScore().stripTrailingZeros());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, hardScore.stripTrailingZeros(), softScore.stripTrailingZeros());
    }

    @Override
    public int compareTo(@NonNull HardSoftBigDecimalScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        }
        int hardScoreComparison = hardScore.compareTo(other.hardScore());
        if (hardScoreComparison != 0) {
            return hardScoreComparison;
        } else {
            return softScore.compareTo(other.softScore());
        }
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0, HARD_LABEL, SOFT_LABEL);
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + hardScore + HARD_LABEL + "/" + softScore + SOFT_LABEL;
    }

}
