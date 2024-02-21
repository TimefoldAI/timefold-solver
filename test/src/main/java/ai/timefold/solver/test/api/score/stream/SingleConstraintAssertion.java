package ai.timefold.solver.test.api.score.stream;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

public interface SingleConstraintAssertion {

    /**
     * As defined by {@link #justifiesWith(ConstraintJustification)}.
     *
     * @param justification the expected justification.
     * @param message sometimes null, description of the scenario being asserted
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    SingleConstraintAssertion justifiesWith(ConstraintJustification justification, String message);

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
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a specific
     * {@link ConstraintJustification}.
     * <p>
     * The justification class types must match; otherwise it fails with no match.
     *
     * @param justification the expected justification.
     * @return never null
     * @throws AssertionError when the expected penalty is not observed
     */
    default SingleConstraintAssertion justifiesWith(ConstraintJustification justification) {
        return justifiesWith(justification, null);
    }

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
        penalizesBy(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(int matchWeightTotal, String message);

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(long matchWeightTotal) {
        penalizesBy(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(long matchWeightTotal, String message);

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(BigDecimal matchWeightTotal) {
        penalizesBy(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #penalizesBy(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(BigDecimal matchWeightTotal, String message);

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
        penalizes(times, null);
    }

    /**
     * As defined by {@link #penalizes(long)}.
     *
     * @param times at least 0, expected number of times that the constraint will penalize
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizes(long times, String message);

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
        rewardsWith(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(int matchWeightTotal, String message);

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(long matchWeightTotal) {
        rewardsWith(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(long matchWeightTotal, String message);

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(BigDecimal matchWeightTotal) {
        rewardsWith(matchWeightTotal, null);
    }

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(BigDecimal matchWeightTotal, String message);

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
        rewards(times, null);
    }

    /**
     * As defined by {@link #rewards(long)}.
     *
     * @param times at least 0, expected number of times that the constraint will reward
     * @param message sometimes null, description of the scenario being asserted
     * @throws AssertionError when the expected reward is not observed
     */
    void rewards(long times, String message);

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
