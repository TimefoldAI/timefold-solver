package ai.timefold.solver.core.api.score.stream.test;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface MultiConstraintAssertion {

    /**
     * Asserts that the {@link ConstraintProvider} under test, given a set of facts, results in a specific {@link Score}.
     *
     * @param score total score calculated for the given set of facts
     * @throws AssertionError when the expected score does not match the calculated score
     */
    default void scores(Score<?> score) {
        scores(score, null);
    }

    /**
     * As defined by {@link #scores(Score)}.
     *
     * @param score total score calculated for the given set of facts
     * @param message description of the scenario being asserted
     * @throws AssertionError when the expected score does not match the calculated score
     */
    void scores(Score<?> score, @Nullable String message);

    /**
     * Returns the {@link Score} produced by all constraints in the {@link ConstraintProvider} for the given set of facts.
     * <p>
     * Unlike {@link #scores(Score)}, this method does not perform any assertion.
     * Instead, it returns the raw score, allowing the caller to compare scores
     * between different scenarios without hard-coding expected values.
     * <p>
     * Usage example:
     *
     * {@snippet :
     * HardSoftScore scoreA = constraintVerifier.verifyThat()
     *         .givenSolution(solutionA)
     *         .getScore();
     * HardSoftScore scoreB = constraintVerifier.verifyThat()
     *         .givenSolution(solutionB)
     *         .getScore();
     * assertThat(scoreA).isGreaterThan(scoreB);
     * }
     *
     * @return the score produced by all constraints for the given facts, never null
     */
    <S extends Score<S>> S getScore();

}
