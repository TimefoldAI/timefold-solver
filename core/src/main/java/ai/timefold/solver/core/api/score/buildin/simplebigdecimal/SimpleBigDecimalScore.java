package ai.timefold.solver.core.api.score.buildin.simplebigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on 1 level of {@link BigDecimal} constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
@NullMarked
public final class SimpleBigDecimalScore implements Score<SimpleBigDecimalScore> {

    public static final SimpleBigDecimalScore ZERO = new SimpleBigDecimalScore(BigDecimal.ZERO);
    public static final SimpleBigDecimalScore ONE = new SimpleBigDecimalScore(BigDecimal.ONE);

    public static SimpleBigDecimalScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(SimpleBigDecimalScore.class, scoreString, "");
        var score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[0]);
        return of(score);
    }

    /**
     * @deprecated Use {@link #of(BigDecimal)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    public static SimpleBigDecimalScore ofUninitialized(int initScore, BigDecimal score) {
        return of(score);
    }

    public static SimpleBigDecimalScore of(BigDecimal score) {
        if (score.signum() == 0) {
            return ZERO;
        } else if (score.equals(BigDecimal.ONE)) {
            return ONE;
        } else {
            return new SimpleBigDecimalScore(score);
        }
    }

    private final BigDecimal score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleBigDecimalScore() {
        this(BigDecimal.ZERO);
    }

    private SimpleBigDecimalScore(BigDecimal score) {
        this.score = score;
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

    @Override
    public SimpleBigDecimalScore add(SimpleBigDecimalScore addend) {
        return of(score.add(addend.score()));
    }

    @Override
    public SimpleBigDecimalScore subtract(SimpleBigDecimalScore subtrahend) {
        return of(score.subtract(subtrahend.score()));
    }

    @Override
    public SimpleBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        var multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return of(score.multiply(multiplicandBigDecimal).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore divide(double divisor) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        var divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return of(score.divide(divisorBigDecimal, score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore power(double exponent) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        var exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return of(score.pow(exponentBigDecimal.intValue()).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public SimpleBigDecimalScore abs() {
        return of(score.abs());
    }

    @Override
    public SimpleBigDecimalScore zero() {
        return ZERO;
    }

    @Override
    public boolean isFeasible() {
        return true;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { score };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SimpleBigDecimalScore other) {
            return score.stripTrailingZeros().equals(other.score().stripTrailingZeros());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return score.stripTrailingZeros().hashCode();
    }

    @Override
    public int compareTo(SimpleBigDecimalScore other) {
        return score.compareTo(other.score());
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0, "");
    }

    @Override
    public String toString() {
        return score.toString();
    }

}
