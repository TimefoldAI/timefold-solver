package ai.timefold.solver.core.api.score.buildin.simple;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

/**
 * This {@link Score} is based on 1 level of int constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleScore implements Score<SimpleScore> {

    public static final SimpleScore ZERO = new SimpleScore(0, 0);
    public static final SimpleScore ONE = new SimpleScore(0, 1);
    private static final SimpleScore MINUS_ONE = new SimpleScore(0, -1);

    public static SimpleScore parseScore(String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleScore.class, scoreString, "");
        int initScore = ScoreUtil.parseInitScore(SimpleScore.class, scoreString, scoreTokens[0]);
        int score = ScoreUtil.parseLevelAsInt(SimpleScore.class, scoreString, scoreTokens[1]);
        return ofUninitialized(initScore, score);
    }

    public static SimpleScore ofUninitialized(int initScore, int score) {
        if (initScore == 0) {
            return of(score);
        }
        return new SimpleScore(initScore, score);
    }

    public static SimpleScore of(int score) {
        return switch (score) {
            case -1 -> MINUS_ONE;
            case 0 -> ZERO;
            case 1 -> ONE;
            default -> new SimpleScore(0, score);
        };
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int initScore;
    private final int score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleScore() {
        this(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private SimpleScore(int initScore, int score) {
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
    public int score() {
        return score;
    }

    /**
     * As defined by {@link #score()}.
     *
     * @deprecated Use {@link #score()} instead.
     */
    @Deprecated(forRemoval = true)
    public int getScore() {
        return score;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public SimpleScore withInitScore(int newInitScore) {
        return ofUninitialized(newInitScore, score);
    }

    @Override
    public SimpleScore add(SimpleScore addend) {
        return ofUninitialized(
                initScore + addend.initScore(),
                score + addend.score());
    }

    @Override
    public SimpleScore subtract(SimpleScore subtrahend) {
        return ofUninitialized(
                initScore - subtrahend.initScore(),
                score - subtrahend.score());
    }

    @Override
    public SimpleScore multiply(double multiplicand) {
        return ofUninitialized(
                (int) Math.floor(initScore * multiplicand),
                (int) Math.floor(score * multiplicand));
    }

    @Override
    public SimpleScore divide(double divisor) {
        return ofUninitialized(
                (int) Math.floor(initScore / divisor),
                (int) Math.floor(score / divisor));
    }

    @Override
    public SimpleScore power(double exponent) {
        return ofUninitialized(
                (int) Math.floor(Math.pow(initScore, exponent)),
                (int) Math.floor(Math.pow(score, exponent)));
    }

    @Override
    public SimpleScore abs() {
        return ofUninitialized(Math.abs(initScore), Math.abs(score));
    }

    @Override
    public SimpleScore zero() {
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
        if (o instanceof SimpleScore other) {
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
    public int compareTo(SimpleScore other) {
        if (initScore != other.initScore()) {
            return Integer.compare(initScore, other.initScore());
        } else {
            return Integer.compare(score, other.score());
        }
    }

    @Override
    public String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.intValue() != 0, "");
    }

    @Override
    public String toString() {
        return ScoreUtil.getInitPrefix(initScore) + score;
    }

}
