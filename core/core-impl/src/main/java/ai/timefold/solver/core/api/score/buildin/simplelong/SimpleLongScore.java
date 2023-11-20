package ai.timefold.solver.core.api.score.buildin.simplelong;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 1 level of long constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleLongScore implements Score<SimpleLongScore> {

    public static final SimpleLongScore ZERO = new SimpleLongScore(0, 0L);
    public static final SimpleLongScore ONE = new SimpleLongScore(0, 1L);
    public static final SimpleLongScore MINUS_ONE = new SimpleLongScore(0, -1L);

    public static SimpleLongScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleLongScore.class, scoreString, "");
        int initScore = ScoreUtil.parseInitScore(SimpleLongScore.class, scoreString, scoreTokens[0]);
        long score = ScoreUtil.parseLevelAsLong(SimpleLongScore.class, scoreString, scoreTokens[1]);
        return ofUninitialized(initScore, score);
    }

    public static SimpleLongScore ofUninitialized(int initScore, long score) {
        if (initScore == 0) {
            return of(score);
        }
        return new SimpleLongScore(initScore, score);
    }

    public static SimpleLongScore of(long score) {
        if (score == -1L) {
            return MINUS_ONE;
        } else if (score == 0L) {
            return ZERO;
        } else if (score == 1L) {
            return ONE;
        } else {
            return new SimpleLongScore(0, score);
        }
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final long score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleLongScore() {
        this(Integer.MIN_VALUE, Long.MIN_VALUE);
    }

    private SimpleLongScore(int initScore, long score) {
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
    public long score() {
        return score;
    }

    /**
     * As defined by {@link #score()}.
     *
     * @deprecated Use {@link #score()} instead.
     */
    @Deprecated(forRemoval = true)
    public long getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public SimpleLongScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, score);
    }

    @Override
    public SimpleLongScore add(SimpleLongScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                score + addend.score());
    }

    @Override
    public SimpleLongScore subtract(SimpleLongScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                score - subtrahend.score());
    }

    @Override
    public SimpleLongScore multiply(double multiplicand) {
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                (long) Math.floor(score * multiplicand));
    }

    @Override
    public SimpleLongScore divide(double divisor) {
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                (long) Math.floor(score / divisor));
    }

    @Override
    public SimpleLongScore power(double exponent) {
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                (long) Math.floor(Math.pow(score, exponent)));
    }

    @Override
    public SimpleLongScore abs() {
        return ofUninitialized(Math.abs(initScore), Math.abs(score));
    }

    @Override
    public SimpleLongScore zero() {
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
        if (o instanceof SimpleLongScore other) {
            return initScore == other.initScore()
                    && score == other.score();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initScore, score);
    }

    @Override
    public int compareTo(SimpleLongScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else {
            return Long.compare(score, other.score());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.longValue() != 0L, "");
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + score;
    }

}
