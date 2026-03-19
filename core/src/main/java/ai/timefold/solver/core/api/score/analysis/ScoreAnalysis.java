package ai.timefold.solver.core.api.score.analysis;

import java.util.Collection;
import java.util.SequencedMap;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the breakdown of a {@link Score} into individual {@link ConstraintAnalysis} instances,
 * one for each constraint.
 *
 * <p>
 * In order to be fully serializable to JSON, {@link MatchAnalysis} instances must be serializable to JSON
 * and that requires any implementations of {@link ConstraintJustification} to be serializable to JSON.
 * This is the responsibility of the user.
 *
 * <p>
 * For deserialization from JSON, the user needs to provide the deserializer themselves.
 * This is due to the fact that, once the {@link ScoreAnalysis} is received over the wire,
 * we no longer know which {@link Score} type or {@link ConstraintJustification} type was used.
 * The user has all of that information in their domain model,
 * and so they are the correct party to provide the deserializer.
 *
 * <p>
 * Note: {@link ScoreAnalysis} is exclusive to Timefold Solver Enterprise Edition.
 *
 * @param <Score_>
 * @see SolutionManager#analyze(Object) Score analysis is typically obtained through SolutionManager.
 */
@NullMarked
public interface ScoreAnalysis<Score_ extends Score<Score_>> {

    /**
     * The overall score of the solution.
     * This is the score that the individual {@link ConstraintAnalysis} instances sum up to.
     *
     * @return score of the solution being analyzed
     */
    Score_ score();

    /**
     * For each constraint identified by its {@link Constraint#getConstraintRef()},
     * the {@link ConstraintAnalysis} that describes the impact of that constraint on the overall score.
     * <p>
     * Zero-weight constraints are never included, they are excluded from score calculation in the first place.
     * Otherwise constraints are always included, even if they have no matches,
     * unless the score analysis represents a diff between two other analyses.
     *
     * <p>
     * In the case of a diff:
     *
     * <ul>
     * <li>If the constraint weight diff is non-zero,
     * or if the score diff for the constraint is non-zero,
     * the constraint diff will be included.</li>
     * <li>
     * Otherwise if constraint matching is disabled ({@link ScoreAnalysisFetchPolicy#FETCH_SHALLOW})
     * or if only match counts are available ({@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}),
     * constraint diff will only be included if it has a non-zero match count diff.
     * </li>
     * <li>
     * Otherwise (when constraint matching is fully enabled with {@link ScoreAnalysisFetchPolicy#FETCH_ALL})
     * the constraint diff will not be included if the diff of its constraint matches is empty.
     * (In other words: when diffing, the analysis for a particular constraint won't be available
     * if we can guarantee that the constraint matches are identical in both analyses.)
     * </li>
     * </ul>
     *
     * @return Entries have a stable iteration order; items are ordered first by {@link ConstraintAnalysis#weight()},
     *         then by {@link ConstraintAnalysis#constraintRef()}.
     */
    SequencedMap<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap();

    /**
     * Indicates whether the solution was fully initialized at the time of analysis.
     *
     * @return isSolutionInitialized true if the solution was fully initialized at the time of analysis.
     */
    boolean isSolutionInitialized();

    /**
     * Performs a lookup on {@link #constraintMap()}.
     * Equivalent to {@code constraintMap().get(constraintRef)}.
     *
     * @return null if no constraint matches of such constraint are present
     */
    @Nullable
    ConstraintAnalysis<Score_> getConstraintAnalysis(ConstraintRef constraintRef);

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}.
     *
     * @return null if no constraint matches of such constraint are present
     */
    @Nullable
    ConstraintAnalysis<Score_> getConstraintAnalysis(String constraintName);

    /**
     * Compare this {@link ScoreAnalysis} to another {@link ScoreAnalysis}
     * and retrieve the difference between them.
     * The comparison is in the direction of {@code this - other}.
     * <p>
     * Example: if {@code this} has a score of 100 and {@code other} has a score of 90,
     * the returned {@link ScoreAnalysis#score} will be 10.
     * If this and other were inverted, the score would have been -10.
     * The same applies to all other properties of {@link ScoreAnalysis}.
     *
     * <p>
     * In order to properly diff {@link MatchAnalysis} against each other,
     * we rely on the user implementing {@link ConstraintJustification} equality correctly.
     * In other words, the diff will consider two justifications equal if the user says they are equal,
     * and it expects the hash code to be consistent with equals.
     *
     * <p>
     * If one {@link ScoreAnalysis} provides {@link MatchAnalysis} and the other doesn't, exception is thrown.
     * Such {@link ScoreAnalysis} instances are mutually incompatible.
     *
     * <p>
     * If {@code this} came from a fully initialized solution,
     * {@link #isSolutionInitialized} will be true.
     * False otherwise.
     */
    ScoreAnalysis<Score_> diff(ScoreAnalysis<Score_> other);

    /**
     * Returns individual {@link ConstraintAnalysis} instances that make up this {@link ScoreAnalysis}.
     *
     * @return equivalent to {@code constraintMap().values()}
     */
    Collection<ConstraintAnalysis<Score_>> constraintAnalyses();

    /**
     * Returns a diagnostic text that explains the solution through the {@link ConstraintAnalysis} API to identify which
     * constraints cause that score quality.
     * The summary will be limited to the first 3 matches per constraint;
     * see {@link #summarize(int)} to specify a custom limit.
     * The summary is built fresh every time the method is called.
     * <p>
     * In case of an {@link Score#isFeasible() infeasible} solution, this can help diagnose the cause of that.
     *
     * <p>
     * Do not parse the return value, its format may change without warning.
     * Instead, provide this information in a UI or a service,
     * use {@link ScoreAnalysis#constraintAnalyses()}
     * and convert those into a domain-specific API.
     */
    String summarize();

    /**
     * Provides a summary of the solution's score analysis as defined by {@link #summarize()},
     * but allows specifying the maximum number of constraint matches to display per constraint.
     * It is possible to specify the maximum number of constraint matches to display per constraint by passing
     * the desired limit as an argument.
     *
     * @param topLimit maximum number of constraint matches to show per constraint.
     *        Use {@link Integer#MAX_VALUE} to show all matches.
     */
    String summarize(int topLimit);

}
