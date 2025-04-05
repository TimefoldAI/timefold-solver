package ai.timefold.solver.core.api.score.buildin.simplebigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on 1 level of {@link BigDecimal} constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleBigDecimalScore implements Score<SimpleBigDecimalScore> {

    public static final @NonNull SimpleBigDecimalScore ZERO = new SimpleBigDecimalScore(BigDecimal.ZERO);
    public static final @NonNull SimpleBigDecimalScore ONE = new SimpleBigDecimalScore(BigDecimal.ONE);

    public static @NonNull SimpleBigDecimalScore parseScore(@NonNull String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleBigDecimalScore.class, scoreString, "");
        BigDecimal score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[1]);
        return of(score);
    }

    /**
     * @deprecated Use {@link #of(BigDecimal)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.21.0")
    public static @NonNull SimpleBigDecimalScore ofUninitialized(int initScore, @NonNull BigDecimal score) {
        return of(score);
    }

    public static @NonNull SimpleBigDecimalScore of(@NonNull BigDecimal score) {
        if (score.signum() == 0) {
            return ZERO;
        } else if (score.equals(BigDecimal.ONE)) {
            return ONE;
        } else {
            return new SimpleBigDecimalScore(score);
        }
    }

    private final @NonNull BigDecimal score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleBigDecimalScore() {
        this(BigDecimal.ZERO);
    }

    private SimpleBigDecimalScore(@NonNull BigDecimal score) {
        this.score = score;
    }

    /**
     * The total of the broken negative constraints and fulfilled positive constraints.
     * Their weight is included in the total.
     * The score is usually a negative number because most use cases only have negative constraints.
     *
     * @return higher is better, usually negative, 0 if no constraints are broken/fulfilled
     */
    public @NonNull BigDecimal score() {
        return score;
    }

    /**
     * As defined by {@link #score()}.
     *
     * @deprecated Use {@link #score()} instead.
     */
    @Deprecated(forRemoval = true)
    public @NonNull BigDecimal getScore() {
        return score;
    }

    @Override
    public @NonNull SimpleBigDecimalScore add(@NonNull SimpleBigDecimalScore addend) {
        return of(score.add(addend.score()));
    }

    @Override
    public @NonNull SimpleBigDecimalScore subtract(@NonNull SimpleBigDecimalScore subtrahend) {
        return of(score.subtract(subtrahend.score()));
    }

    @Override
    public @NonNull SimpleBigDecimalScore multiply(double multiplicand) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal multiplicandBigDecimal = BigDecimal.valueOf(multiplicand);
        // The (unspecified) scale/precision of the multiplicand should have no impact on the returned scale/precision
        return of(score.multiply(multiplicandBigDecimal).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull SimpleBigDecimalScore divide(double divisor) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal divisorBigDecimal = BigDecimal.valueOf(divisor);
        // The (unspecified) scale/precision of the divisor should have no impact on the returned scale/precision
        return of(score.divide(divisorBigDecimal, score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull SimpleBigDecimalScore power(double exponent) {
        // Intentionally not taken "new BigDecimal(multiplicand, MathContext.UNLIMITED)"
        // because together with the floor rounding it gives unwanted behaviour
        BigDecimal exponentBigDecimal = BigDecimal.valueOf(exponent);
        // The (unspecified) scale/precision of the exponent should have no impact on the returned scale/precision
        // TODO FIXME remove .intValue() so non-integer exponents produce correct results
        // None of the normal Java libraries support BigDecimal.pow(BigDecimal)
        return of(score.pow(exponentBigDecimal.intValue()).setScale(score.scale(), RoundingMode.FLOOR));
    }

    @Override
    public @NonNull SimpleBigDecimalScore abs() {
        return of(score.abs());
    }

    @Override
    public @NonNull SimpleBigDecimalScore zero() {
        return ZERO;
    }

    @Override
    public boolean isFeasible() {
        return true;
    }

    @Override
    public @NonNull Number @NonNull [] toLevelNumbers() {
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
    public int compareTo(@NonNull SimpleBigDecimalScore other) {
        return score.compareTo(other.score());
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildShortString(this, n -> ((BigDecimal) n).compareTo(BigDecimal.ZERO) != 0, "");
    }

    @Override
    public String toString() {
        return score.toString();
    }

}
