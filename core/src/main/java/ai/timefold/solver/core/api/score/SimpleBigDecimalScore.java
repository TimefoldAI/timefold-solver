package ai.timefold.solver.core.api.score;

import static ai.timefold.solver.core.impl.score.ScoreUtil.STRUCTURAL_LABEL;

import java.math.BigDecimal;

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

    public static final SimpleBigDecimalScore ZERO = new SimpleBigDecimalScore(BigDecimal.ZERO);
    public static final SimpleBigDecimalScore ONE = new SimpleBigDecimalScore(BigDecimal.ONE);

    public SimpleBigDecimalScore(BigDecimal score) {
        this(0L, score);
    }

    public static SimpleBigDecimalScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(SimpleBigDecimalScore.class, scoreString, "");
        if (scoreTokens.length == 1) {
            var score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[0]);
            return of(score);
        } else {
            var structuralScore = ScoreUtil.parseLevelAsLong(SimpleBigDecimalScore.class, scoreString, scoreTokens[0]);
            var score = ScoreUtil.parseLevelAsBigDecimal(SimpleBigDecimalScore.class, scoreString, scoreTokens[1]);
            return new SimpleBigDecimalScore(structuralScore, score);
        }
    }

    public static SimpleBigDecimalScore of(BigDecimal score) {
        if (ScoreUtil.isZero(score)) {
            return ZERO;
        }
        return new SimpleBigDecimalScore(score);
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
        return of(ScoreUtil.multiply(score, multiplicand));
    }

    @Override
    public SimpleBigDecimalScore divide(double divisor) {
        return of(ScoreUtil.divide(score, divisor));
    }

    @Override
    public SimpleBigDecimalScore power(double exponent) {
        return of(ScoreUtil.power(score, exponent));
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
                    && ScoreUtil.equalsIgnoringScale(score, otherScore);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(structuralScore) ^ ScoreUtil.hashCodeIgnoringScale(score);
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
        return ScoreUtil.buildShortString(this, ScoreUtil.BIG_DECIMAL_NOT_ZERO, "");
    }

    @Override
    public String toString() {
        return (structuralScore < 0) ? "%d%s/%s".formatted(structuralScore, STRUCTURAL_LABEL, score) : score.toString();
    }

}
