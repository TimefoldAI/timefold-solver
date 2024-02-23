package ai.timefold.solver.test.api.score.stream;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public interface SingleConstraintAssertion {

    /**
     * As defined by {@link #justifiesWith(ConstraintJustification...)}.
     *
     * @param justifications the expected justification.
     * @param message sometimes null, description of the scenario being asserted
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    SingleConstraintAssertion justifiesWith(String message, ConstraintJustification... justifications);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a specific
     * {@link ConstraintJustification}.
     * <p>
     * The justification class types must match; otherwise it fails with no match.
     *
     * @param justifications the expected justifications.
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    default SingleConstraintAssertion justifiesWith(ConstraintJustification... justifications) {
        return justifiesWith(null, justifications);
    }

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a specific indictments.
     *
     * @param indictments the expected indictments.
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    default SingleConstraintAssertion indictsWith(Object... indictments) {
        return indictsWith(null, indictments);
    }

    /**
     * As defined by {@link #indictsWith(Object...)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param indictments the expected indictments.
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    SingleConstraintAssertion indictsWith(String message, Object... indictments);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a specific penalty.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example: a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}. In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(int matchWeightTotal) {
        penalizesBy(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     * @deprecated Use {@link #penalizesBy(String, int)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void penalizesBy(int matchWeightTotal, String message) {
        penalizesBy(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(String message, int matchWeightTotal);

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(long matchWeightTotal) {
        penalizesBy(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(long)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     *
     * @deprecated Use {@link #penalizesBy(String, long)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void penalizesBy(long matchWeightTotal, String message) {
        penalizesBy(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(long)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(String message, long matchWeightTotal);

    /**
     * As defined by {@link #penalizesBy(long)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(BigDecimal matchWeightTotal) {
        penalizesBy(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(BigDecimal)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     *
     * @deprecated Use {@link #penalizesBy(String, BigDecimal)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void penalizesBy(BigDecimal matchWeightTotal, String message) {
        penalizesBy(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesBy(BigDecimal)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(String message, BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a given number of penalties.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example: if there are two matches with weight of {@code 10} each, this assertion will check for 2 matches.
     *
     * @param times at least 0, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizes(long times) {
        penalizes(null, times);
    }

    /**
     * As defined by {@link #penalizes(long)}.
     *
     * @param times at least 0, expected number of times that the constraint will penalize
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     *
     * @deprecated Use {@link #penalizes(String, long)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void penalizes(long times, String message) {
        penalizes(message, times);
    }

    /**
     * As defined by {@link #penalizes(long)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizes(String message, long times);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in any number of penalties.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example: if there are two matches with weight of {@code 10} each, this assertion will succeed.
     * If there are no matches, it will fail.
     *
     * @throws AssertionError when there are no penalties
     */
    default void penalizes() {
        penalizes(null);
    }

    /**
     * As defined by {@link #penalizes()}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when there are no penalties
     */
    void penalizes(String message);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a specific reward.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example: a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}. In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(int matchWeightTotal) {
        rewardsWith(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     *
     * @deprecated Use {@link #rewardsWith(String, int)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void rewardsWith(int matchWeightTotal, String message) {
        rewardsWith(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(String message, int matchWeightTotal);

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(long matchWeightTotal) {
        rewardsWith(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(long)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     *
     * @deprecated Use {@link #rewardsWith(String, long)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void rewardsWith(long matchWeightTotal, String message) {
        rewardsWith(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(long)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(String message, long matchWeightTotal);

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(BigDecimal matchWeightTotal) {
        rewardsWith(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(BigDecimal)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     *
     * @deprecated Use {@link #rewardsWith(String, BigDecimal)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void rewardsWith(BigDecimal matchWeightTotal, String message) {
        rewardsWith(message, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWith(BigDecimal)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(String message, BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a given number of rewards.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example: if there are two matches with weight of {@code 10} each, this assertion will check for 2 matches.
     *
     * @param times at least 0, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewards(long times) {
        rewards(null, times);
    }

    /**
     * As defined by {@link #rewards(long)}.
     *
     * @param times at least 0, expected number of times that the constraint will reward
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     *
     * @deprecated Use {@link #rewards(String, long)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.8.0")
    default void rewards(long times, String message) {
        rewards(message, times);
    }

    /**
     * As defined by {@link #rewards(long)}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    void rewards(String message, long times);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in any number of rewards.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example: if there are two matches with weight of {@code 10} each, this assertion will succeed.
     * If there are no matches, it will fail.
     *
     * @throws AssertionError when there are no rewards
     */
    default void rewards() {
        rewards(null);
    }

    /**
     * As defined by {@link #rewards()}.
     *
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when there are no rewards
     */
    void rewards(String message);

}
