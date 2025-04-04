package ai.timefold.solver.core.api.score.buildin.simple;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on 1 level of int constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleScore implements Score<SimpleScore> {

    public static final SimpleScore ZERO = new SimpleScore(0);
    public static final SimpleScore ONE = new SimpleScore(1);
    private static final SimpleScore MINUS_ONE = new SimpleScore(-1);

    public static @NonNull SimpleScore parseScore(@NonNull String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleScore.class, scoreString, "");
        int score = ScoreUtil.parseLevelAsInt(SimpleScore.class, scoreString, scoreTokens[1]);
        return of(score);
    }

    /**
     * @deprecated Use {@link #of(int)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.21.0")
    public static @NonNull SimpleScore ofUninitialized(int initScore, int score) {
        return of(score);
    }

    public static @NonNull SimpleScore of(int score) {
        return switch (score) {
            case -1 -> MINUS_ONE;
            case 0 -> ZERO;
            case 1 -> ONE;
            default -> new SimpleScore(score);
        };
    }

    private final int score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleScore() {
        this(Integer.MIN_VALUE);
    }

    private SimpleScore(int score) {
        this.score = score;
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

    @Override
    public @NonNull SimpleScore add(@NonNull SimpleScore addend) {
        return of(score + addend.score());
    }

    @Override
    public @NonNull SimpleScore subtract(@NonNull SimpleScore subtrahend) {
        return of(score - subtrahend.score());
    }

    @Override
    public @NonNull SimpleScore multiply(double multiplicand) {
        return of((int) Math.floor(score * multiplicand));
    }

    @Override
    public @NonNull SimpleScore divide(double divisor) {
        return of((int) Math.floor(score / divisor));
    }

    @Override
    public @NonNull SimpleScore power(double exponent) {
        return of((int) Math.floor(Math.pow(score, exponent)));
    }

    @Override
    public @NonNull SimpleScore abs() {
        return of(Math.abs(score));
    }

    @Override
    public @NonNull SimpleScore zero() {
        return ZERO;
    }

    @Override
    public boolean isFeasible() {
        return true;
    }

    @Override
    public Number @NonNull [] toLevelNumbers() {
        return new Number[] { score };
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SimpleScore other) {
            return score == other.score();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(score);
    }

    @Override
    public int compareTo(@NonNull SimpleScore other) {
        return Integer.compare(score, other.score());
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.intValue() != 0, "");
    }

    @Override
    public String toString() {
        return Integer.toString(score);
    }

}
