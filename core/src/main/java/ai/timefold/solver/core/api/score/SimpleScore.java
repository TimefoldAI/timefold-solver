package ai.timefold.solver.core.api.score;

import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NullMarked;

/**
 * This {@link Score} is based on 1 level of long constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
@NullMarked
public record SimpleScore(long score) implements Score<SimpleScore> {

    public static final SimpleScore ZERO = new SimpleScore(0L);
    public static final SimpleScore ONE = new SimpleScore(1L);
    public static final SimpleScore MINUS_ONE = new SimpleScore(-1L);

    public static SimpleScore parseScore(String scoreString) {
        var scoreTokens = ScoreUtil.parseScoreTokens(SimpleScore.class, scoreString, "");
        var score = ScoreUtil.parseLevelAsLong(SimpleScore.class, scoreString, scoreTokens[0]);
        return of(score);
    }

    public static SimpleScore of(long score) {
        if (score == -1L) {
            return MINUS_ONE;
        } else if (score == 0L) {
            return ZERO;
        } else if (score == 1L) {
            return ONE;
        } else {
            return new SimpleScore(score);
        }
    }

    @Override
    public SimpleScore add(SimpleScore addend) {
        return of(score + addend.score());
    }

    @Override
    public SimpleScore subtract(SimpleScore subtrahend) {
        return of(score - subtrahend.score());
    }

    @Override
    public SimpleScore multiply(double multiplicand) {
        return of((long) Math.floor(score * multiplicand));
    }

    @Override
    public SimpleScore divide(double divisor) {
        return of((long) Math.floor(score / divisor));
    }

    @Override
    public SimpleScore power(double exponent) {
        return of((long) Math.floor(Math.pow(score, exponent)));
    }

    @Override
    public SimpleScore abs() {
        return of(Math.abs(score));
    }

    @Override
    public SimpleScore zero() {
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
        if (o instanceof SimpleScore(var otherScore)) {
            return score == otherScore;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(score);
    }

    @Override
    public int compareTo(SimpleScore other) {
        return Long.compare(score, other.score());
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.longValue() != 0L, "");
    }

    @Override
    public String toString() {
        return Long.toString(score);
    }

}
