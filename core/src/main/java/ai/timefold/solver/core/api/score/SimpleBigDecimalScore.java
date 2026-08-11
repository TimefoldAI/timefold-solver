package ai.timefold.solver.core.api.score;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
public record SimpleBigDecimalScore(long structuralScore, BigDecimal score) implements Score<SimpleBigDecimalScore> {

    public static final SimpleBigDecimalScore INVALID = new SimpleBigDecimalScore(-1L, BigDecimal.ZERO);
    public static final SimpleBigDecimalScore ZERO = new SimpleBigDecimalScore(BigDecimal.ZERO);
    public static final SimpleBigDecimalScore ONE = new SimpleBigDecimalScore(BigDecimal.ONE);

    public SimpleBigDecimalScore(BigDecimal score) {
        this(0L, score);
    }

    public static SimpleBigDecimalScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(SimpleBigDecimalScore.class, scoreString, "");
        var score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[0]);
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
        return structuralScore >= 0;
    }

    @Override
    public Number[] toLevelNumbers() {
        return new Number[] { score };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SimpleBigDecimalScore(var otherStructuralScore, var otherScore)) {
            return structuralScore == otherStructuralScore
                    && score.stripTrailingZeros().equals(otherScore.stripTrailingZeros());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(structuralScore) ^ score.stripTrailingZeros().hashCode();
    }

    @Override
    public int compareTo(SimpleBigDecimalScore other) {
        if (structuralScore != other.structuralScore) {
            return Long.compare(structuralScore, other.structuralScore);
        }
        return score.compareTo(other.score());
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0, "");
    }

    @Override
    public String toString() {
        return (structuralScore < 0) ? "invalid" : score.toString();
    }

}
