package ai.timefold.solver.core.api.score.buildin.simplebigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 1 level of {@link BigDecimal} constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleBigDecimalScore implements Score<SimpleBigDecimalScore> {

    public static final SimpleBigDecimalScore ZERO = new SimpleBigDecimalScore(0, BigDecimal.ZERO);
    public static final SimpleBigDecimalScore ONE = new SimpleBigDecimalScore(0, BigDecimal.ONE);

    public static SimpleBigDecimalScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleBigDecimalScore.class, scoreString, "");
        int initScore = ScoreUtil.parseInitScore(SimpleBigDecimalScore.class, scoreString, scoreTokens[0]);
        BigDecimal score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[1]);
        return ofUninitialized(initScore, score);
    }

    public static SimpleBigDecimalScore ofUninitialized(int initScore, BigDecimal score) {
        if (initScore == 0) {
            return of(score);
        }
        return new SimpleBigDecimalScore(initScore, score);
    }

    public static SimpleBigDecimalScore of(BigDecimal score) {
        if (score.signum() == 0) {
            return ZERO;
        } else if (score.equals(BigDecimal.ONE)) {
            return ONE;
        } else {
            return new SimpleBigDecimalScore(0, score);
        }
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final BigDecimal score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleBigDecimalScore() {
        this(Integer.MIN_VALUE, null);
    }

    private SimpleBigDecimalScore(int initScore, BigDecimal score) {
        this.initScore = initScore;
        this.score = score;
    }

    @Override
    public int initScore() {
        return initScore;
    }

    /**
     * The total of the broken negative constraints and fulfilled positive constraints.
     * Their weight is included in the total.
     * The score is usually a negative number because most use cases only have negative constraints.
     *
     * @return higher is better, usually negative, 0 if no constraints are broken/fulfilled
     */
    public BigDecimal score() {
        return score;
    }

    /**
     * As defined by {@link #score()}.
     *
     * @deprecated Use {@link #score()} instead.
     */
    @Deprecated(forRemoval = true)
    public BigDecimal getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public SimpleBigDecimalScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, score);
    }

    @Override
    public SimpleBigDecimalScore add(SimpleBigDecimalScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                score.add(addend.score()));
    }

    @Override
    public SimpleBigDecimalScore subtract(SimpleBigDecimalScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                score.subtract(subtrahend.score()));
    }

    @Override
    public SimpleBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                score.multiply(multiplicandBigDecimal).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore divide(double divisor) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                score.divide(divisorBigDecimal, score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore power(double exponent) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                score.pow(exponentBigDecimal.intValue()).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore abs() {
        return ofUninitialized(Math.abs(initScore), score.abs());
    }

    @Override
    public SimpleBigDecimalScore zero() {
        return ZERO;
    }

    @Override
    public boolean isFeasible() {
        return initScore >= 0;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { score };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SimpleBigDecimalScore other) {
            return initScore == other.initScore()
                    && score.stripTrailingZeros().equals(other.score().stripTrailingZeros());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, score.stripTrailingZeros());
    }

    @Override
    public int compareTo(SimpleBigDecimalScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else {
            return score.compareTo(other.score());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0, "");
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + score;
    }

}
