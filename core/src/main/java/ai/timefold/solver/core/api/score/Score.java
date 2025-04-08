package ai.timefold.solver.core.api.score;

import java.io.Serializable;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;

import org.jspecify.annotations.NullMarked;

/**
 * A Score is result of the score function (AKA fitness function) on a single possible solution.
 *
 * Implementations must be immutable,
 * preferably a Java record or even a primitive record,
 * if the target JDK permits that.
 *
 * @param <Score_> the actual score type to allow addition, subtraction and other arithmetic
 * @see HardSoftScore
 */
@NullMarked
public interface Score<Score_ extends Score<Score_>>
        extends Comparable<Score_>, Serializable {

    /**
     * @return Always zero.
     * @deprecated No point in using this method anymore.
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    default int initScore() {
        return 0;
    }

    /**
     * @return Always zero.
     * @deprecated No point in using this method anymore.
     */
    @Deprecated(forRemoval = true)
    default int getInitScore() {
        return 0;
    }

    /**
     * @return this, init score always zero.
     * @deprecated No point in using this method anymore.
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    @SuppressWarnings("unchecked")
    default Score_ withInitScore(int newInitScore) {
        return (Score_) this;
    }

    /**
     * Returns a Score whose value is (this + addend).
     *
     * @param addend value to be added to this Score
     * @return this + addend
     */
    Score_ add(Score_ addend);

    /**
     * Returns a Score whose value is (this - subtrahend).
     *
     * @param subtrahend value to be subtracted from this Score
     * @return this - subtrahend, rounded as necessary
     */
    Score_ subtract(Score_ subtrahend);

    /**
     * Returns a Score whose value is (this * multiplicand).
     * When rounding is needed, it should be floored (as defined by {@link Math#floor(double)}).
     * <p>
     * If the implementation has a scale/precision, then the unspecified scale/precision of the double multiplicand
     * should have no impact on the returned scale/precision.
     *
     * @param multiplicand value to be multiplied by this Score.
     * @return this * multiplicand
     */
    Score_ multiply(double multiplicand);

    /**
     * Returns a Score whose value is (this / divisor).
     * When rounding is needed, it should be floored (as defined by {@link Math#floor(double)}).
     * <p>
     * If the implementation has a scale/precision, then the unspecified scale/precision of the double divisor
     * should have no impact on the returned scale/precision.
     *
     * @param divisor value by which this Score is to be divided
     * @return this / divisor
     */
    Score_ divide(double divisor);

    /**
     * Returns a Score whose value is (this ^ exponent).
     * When rounding is needed, it should be floored (as defined by {@link Math#floor(double)}).
     * <p>
     * If the implementation has a scale/precision, then the unspecified scale/precision of the double exponent
     * should have no impact on the returned scale/precision.
     *
     * @param exponent value by which this Score is to be powered
     * @return this ^ exponent
     */
    Score_ power(double exponent);

    /**
     * Returns a Score whose value is (- this).
     *
     * @return - this
     */
    @SuppressWarnings("unchecked")
    default Score_ negate() {
        var zero = zero();
        var current = (Score_) this;
        if (zero.equals(current)) {
            return current;
        }
        return zero.subtract(current);
    }

    /**
     * Returns a Score whose value is the absolute value of the score, i.e. |this|.
     */
    Score_ abs();

    /**
     * Returns a Score, all levels of which are zero.
     */
    Score_ zero();

    /**
     *
     * @return true when this {@link Object#equals(Object) is equal to} {@link #zero()}.
     */
    default boolean isZero() {
        return this.equals(zero());
    }

    /**
     * Returns an array of numbers representing the Score. Each number represents 1 score level.
     * A greater score level uses a lower array index than a lesser score level.
     * <p>
     * When rounding is needed, each rounding should be floored (as defined by {@link Math#floor(double)}).
     * The length of the returned array must be stable for a specific {@link Score} implementation.
     * <p>
     * For example: {@code -0hard/-7soft} returns {@code new int{-0, -7}}
     */
    Number[] toLevelNumbers();

    /**
     * As defined by {@link #toLevelNumbers()}, only returns double[] instead of Number[].
     */
    default double[] toLevelDoubles() {
        var levelNumbers = toLevelNumbers();
        var levelDoubles = new double[levelNumbers.length];
        for (var i = 0; i < levelNumbers.length; i++) {
            levelDoubles[i] = levelNumbers[i].doubleValue();
        }
        return levelDoubles;
    }

    /**
     * @return always true
     * @deprecated No point in using this method anymore.
     */
    @Deprecated(forRemoval = true, since = "1.22.0")
    default boolean isSolutionInitialized() {
        return true;
    }

    /**
     * A {@link PlanningSolution} is feasible if it has no broken hard constraints.
     * Simple scores ({@link SimpleScore}, {@link SimpleLongScore}, {@link SimpleBigDecimalScore}) are always feasible.
     *
     * @return true if the hard score is 0 or higher.
     */
    boolean isFeasible();

    /**
     * Like {@link Object#toString()}, but trims score levels which have a zero weight.
     * For example {@literal 0hard/-258soft} returns {@literal -258soft}.
     * <p>
     * Do not use this format to persist information as text, use {@link Object#toString()} instead,
     * so it can be parsed reliably.
     */
    String toShortString();

}
