package ai.timefold.solver.test.api.score.stream;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface SingleConstraintAssertion {

    /**
     * As defined by {@link #justifiesWith(ConstraintJustification...)}.
     *
     * @param justifications the expected justification.
     * @param message description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    @NonNull
    SingleConstraintAssertion justifiesWith(@Nullable String message,
            @NonNull ConstraintJustification @NonNull... justifications);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a given
     * {@link ConstraintJustification}.
     *
     * @param justifications the expected justifications.
     * @throws AssertionError when the expected penalty is not observed
     */
    default @NonNull SingleConstraintAssertion justifiesWith(@NonNull ConstraintJustification @NonNull... justifications) {
        return justifiesWith(null, justifications);
    }

    /**
     * As defined by {@link #justifiesWithExactly(ConstraintJustification...)}.
     *
     * @param justifications the expected justification.
     * @param message description of the scenario being asserted
     * @throws AssertionError when the expected penalty is not observed
     */
    @NonNull
    SingleConstraintAssertion justifiesWithExactly(@Nullable String message,
            @NonNull ConstraintJustification @NonNull... justifications);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in a given
     * {@link ConstraintJustification} and nothing else.
     *
     * @param justifications the expected justifications.
     * @throws AssertionError when the expected penalty is not observed
     */
    default @NonNull SingleConstraintAssertion
            justifiesWithExactly(@NonNull ConstraintJustification @NonNull... justifications) {
        return justifiesWithExactly(null, justifications);
    }

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in the given indictments.
     *
     * @param indictments the expected indictments.
     * @throws AssertionError when the expected penalty is not observed
     */
    default @NonNull SingleConstraintAssertion indictsWith(@NonNull Object @NonNull... indictments) {
        return indictsWith(null, indictments);
    }

    /**
     * As defined by {@link #indictsWith(Object...)}.
     *
     * @param message description of the scenario being asserted
     * @param indictments the expected indictments.
     * @throws AssertionError when the expected penalty is not observed
     */
    @NonNull
    SingleConstraintAssertion indictsWith(@Nullable String message, @NonNull Object @NonNull... indictments);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts, results in the given indictments and
     * nothing else.
     *
     * @param indictments the expected indictments.
     * @throws AssertionError when the expected penalty is not observed
     */
    default @NonNull SingleConstraintAssertion indictsWithExactly(@NonNull Object @NonNull... indictments) {
        return indictsWithExactly(null, indictments);
    }

    /**
     * As defined by {@link #indictsWithExactly(Object...)}.
     *
     * @param message description of the scenario being asserted
     * @param indictments the expected indictments.
     * @throws AssertionError when the expected penalty is not observed
     */
    @NonNull
    SingleConstraintAssertion indictsWithExactly(@Nullable String message, @NonNull Object @NonNull... indictments);

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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(@Nullable String message, int matchWeightTotal);

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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #penalizesBy(long)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesBy(@NonNull BigDecimal matchWeightTotal) {
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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesBy(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

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
     * @param message description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizes(@Nullable String message, long times);

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
     * @param message description of the scenario being asserted
     * @throws AssertionError when there are no penalties
     */
    void penalizes(@Nullable String message);

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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(@Nullable String message, int matchWeightTotal);

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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #rewardsWith(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWith(@NonNull BigDecimal matchWeightTotal) {
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
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWith(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

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
     * @param message description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    void rewards(@Nullable String message, long times);

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
     * @param message description of the scenario being asserted
     * @throws AssertionError when there are no rewards
     */
    void rewards(@Nullable String message);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a specific penalty larger than given.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example:
     * a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}.
     * In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByMoreThan(int matchWeightTotal) {
        penalizesByMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByMoreThan(int)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByMoreThan(@Nullable String message, int matchWeightTotal);

    /**
     * As defined by {@link #penalizesByMoreThan(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByMoreThan(long matchWeightTotal) {
        penalizesByMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByMoreThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByMoreThan(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #penalizesByMoreThan(long)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByMoreThan(@NonNull BigDecimal matchWeightTotal) {
        penalizesByMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByMoreThan(BigDecimal)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByMoreThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a number of rewards larger than given.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches.
     * For example:
     * if there are two matches with weight of {@code 10} each,
     * this assertion will check for 2 matches.
     *
     * @param times at least 0, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesMoreThan(long times) {
        penalizesMoreThan(null, times);
    }

    /**
     * As defined by {@link #penalizesMoreThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesMoreThan(@Nullable String message, long times);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a specific reward larger than given.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example: a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}.
     * In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithMoreThan(int matchWeightTotal) {
        rewardsWithMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithMoreThan(int)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithMoreThan(@Nullable String message, int matchWeightTotal);

    /**
     * As defined by {@link #rewardsWithMoreThan(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithMoreThan(long matchWeightTotal) {
        rewardsWithMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithMoreThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithMoreThan(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #rewardsWithMoreThan(int)}.
     *
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithMoreThan(@NonNull BigDecimal matchWeightTotal) {
        rewardsWithMoreThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithMoreThan(BigDecimal)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 0, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithMoreThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a number of rewards larger than given.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example:
     * if there are two matches with weight of {@code 10} each,
     * this assertion will check for 2 matches.
     *
     * @param times at least 0, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsMoreThan(long times) {
        rewardsMoreThan(null, times);
    }

    /**
     * As defined by {@link #rewardsMoreThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param times at least 0, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsMoreThan(@Nullable String message, long times);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a specific penalty smaller than given.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example:
     * a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}.
     * In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByLessThan(int matchWeightTotal) {
        penalizesByLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByLessThan(int)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByLessThan(@Nullable String message, int matchWeightTotal);

    /**
     * As defined by {@link #penalizesByLessThan(int)}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByLessThan(long matchWeightTotal) {
        penalizesByLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByLessThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByLessThan(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #penalizesByLessThan(long)}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesByLessThan(@NonNull BigDecimal matchWeightTotal) {
        penalizesByLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #penalizesByLessThan(BigDecimal)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesByLessThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a number of rewards smaller than given.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches.
     * For example:
     * if there are two matches with weight of {@code 10} each,
     * this assertion will check for 2 matches.
     *
     * @param times at least 1, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    default void penalizesLessThan(long times) {
        penalizesLessThan(null, times);
    }

    /**
     * As defined by {@link #penalizesLessThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param times at least 1, expected number of times that the constraint will penalize
     * @throws AssertionError when the expected penalty is not observed
     */
    void penalizesLessThan(@Nullable String message, long times);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a specific reward smaller than given.
     * <p>
     * Ignores the constraint weight: it only asserts the match weights.
     * For example: a match with a match weight of {@code 10} on a constraint with a constraint weight of {@code -2hard}
     * reduces the score by {@code -20hard}.
     * In that case, this assertion checks for {@code 10}.
     * <p>
     * An {@code int matchWeightTotal} automatically casts to {@code long} for {@link HardSoftLongScore long scores}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithLessThan(int matchWeightTotal) {
        rewardsWithLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithLessThan(int)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithLessThan(@Nullable String message, int matchWeightTotal);

    /**
     * As defined by {@link #rewardsWithLessThan(int)}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithLessThan(long matchWeightTotal) {
        rewardsWithLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithLessThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithLessThan(@Nullable String message, long matchWeightTotal);

    /**
     * As defined by {@link #rewardsWithLessThan(int)}.
     *
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsWithLessThan(@NonNull BigDecimal matchWeightTotal) {
        rewardsWithLessThan(null, matchWeightTotal);
    }

    /**
     * As defined by {@link #rewardsWithLessThan(BigDecimal)}.
     *
     * @param message description of the scenario being asserted
     * @param matchWeightTotal at least 1, expected sum of match weights of matches of the constraint.
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsWithLessThan(@Nullable String message, @NonNull BigDecimal matchWeightTotal);

    /**
     * Asserts that the {@link Constraint} being tested, given a set of facts,
     * results in a number of rewards smaller than given.
     * <p>
     * Ignores the constraint and match weights: it only asserts the number of matches
     * For example:
     * if there are two matches with weight of {@code 10} each,
     * this assertion will check for 2 matches.
     *
     * @param times at least 1, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    default void rewardsLessThan(long times) {
        rewardsLessThan(null, times);
    }

    /**
     * As defined by {@link #rewardsLessThan(long)}.
     *
     * @param message description of the scenario being asserted
     * @param times at least 1, expected number of times that the constraint will reward
     * @throws AssertionError when the expected reward is not observed
     */
    void rewardsLessThan(@Nullable String message, long times);

}
