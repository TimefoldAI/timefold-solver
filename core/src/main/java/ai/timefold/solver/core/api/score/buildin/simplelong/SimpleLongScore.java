package ai.timefold.solver.core.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.ScoreUtil;

import org.jspecify.annotations.NonNull;

/**
 * This {@link Score} is based on 1 level of long constraints.
 * <p>
 * This class is immutable.
 *
 * @see Score
 */
public final class SimpleLongScore implements Score<SimpleLongScore> {

    public static final SimpleLongScore ZERO = new SimpleLongScore(0L);
    public static final SimpleLongScore ONE = new SimpleLongScore(1L);
    public static final SimpleLongScore MINUS_ONE = new SimpleLongScore(-1L);

    public static @NonNull SimpleLongScore parseScore(@NonNull String scoreString) {
        String[] scoreTokens = ScoreUtil.parseScoreTokens(SimpleLongScore.class, scoreString, "");
        long score = ScoreUtil.parseLevelAsLong(SimpleLongScore.class, scoreString, scoreTokens[1]);
        return of(score);
    }

    /**
     * @deprecated Use {@link #of(long)} instead.
     * @return init score is always zero
     */
    @Deprecated(forRemoval = true, since = "1.21.0")
    public static @NonNull SimpleLongScore ofUninitialized(int initScore, long score) {
        return of(score);
    }

    public static @NonNull SimpleLongScore of(long score) {
        if (score == -1L) {
            return MINUS_ONE;
        } else if (score == 0L) {
            return ZERO;
        } else if (score == 1L) {
            return ONE;
        } else {
            return new SimpleLongScore(score);
        }
    }

    private final long score;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * timefold-solver-jpa, timefold-solver-jackson, timefold-solver-jaxb, ...
     */
    @SuppressWarnings("unused")
    private SimpleLongScore() {
        this(Long.MIN_VALUE);
    }

    private SimpleLongScore(long score) {
        this.score = score;
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

    @Override
    public @NonNull SimpleLongScore add(@NonNull SimpleLongScore addend) {
        return of(score + addend.score());
    }

    @Override
    public @NonNull SimpleLongScore subtract(@NonNull SimpleLongScore subtrahend) {
        return of(score - subtrahend.score());
    }

    @Override
    public @NonNull SimpleLongScore multiply(double multiplicand) {
        return of((long) Math.floor(score * multiplicand));
    }

    @Override
    public @NonNull SimpleLongScore divide(double divisor) {
        return of((long) Math.floor(score / divisor));
    }

    @Override
    public @NonNull SimpleLongScore power(double exponent) {
        return of((long) Math.floor(Math.pow(score, exponent)));
    }

    @Override
    public @NonNull SimpleLongScore abs() {
        return of(Math.abs(score));
    }

    @Override
    public @NonNull SimpleLongScore zero() {
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
        if (o instanceof SimpleLongScore other) {
            return score == other.score();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(score);
    }

    @Override
    public int compareTo(@NonNull SimpleLongScore other) {
        return Long.compare(score, other.score());
    }

    @Override
    public @NonNull String toShortString() {
        return ScoreUtil.buildShortString(this, n -> n.longValue() != 0L, "");
    }

    @Override
    public String toString() {
        return Long.toString(score);
    }

}
