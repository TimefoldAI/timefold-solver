package ai.timefold.solver.core.api.score.analysis;

import static java.util.Comparator.comparing;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents the breakdown of a {@link Score} into individual {@link ConstraintAnalysis} instances,
 * one for each constraint.
 * Compared to {@link ScoreExplanation}, this is JSON-friendly and faster to generate.
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
 * Note: the constructors of this record are off-limits.
 * We ask users to use exclusively {@link SolutionManager#analyze(Object)} to obtain instances of this record.
 *
 * @param score Score of the solution being analyzed.
 * @param constraintMap for each constraint identified by its {@link Constraint#getConstraintRef()},
 *        the {@link ConstraintAnalysis} that describes the impact of that constraint on the overall score.
 *        <p>
 *        Zero-weight constraints are never included, they are excluded from score calculation in the first place.
 *        Otherwise constraints are always included, even if they have no matches,
 *        unless the score analysis represents a diff between two other analyses.
 * 
 *        <p>
 *        In the case of a diff:
 * 
 *        <ul>
 *        <li>If the constraint weight diff is non-zero,
 *        or if the score diff for the constraint is non-zero,
 *        the constraint diff will be included.</li>
 *        <li>
 *        Otherwise if constraint matching is disabled ({@link ScoreAnalysisFetchPolicy#FETCH_SHALLOW})
 *        or if only match counts are available ({@link ScoreAnalysisFetchPolicy#FETCH_MATCH_COUNT}),
 *        constraint diff will only be included if it has a non-zero match count diff.
 *        </li>
 *        <li>
 *        Otherwise (when constraint matching is fully enabled with {@link ScoreAnalysisFetchPolicy#FETCH_ALL})
 *        the constraint diff will not be included if the diff of its constraint matches is empty.
 *        (In other words: when diffing, the analysis for a particular constraint won't be available
 *        if we can guarantee that the constraint matches are identical in both analyses.)
 *        </li>
 *        </ul>
 * 
 *        <p>
 *        Entries in the map have a stable iteration order; items are ordered first by {@link ConstraintAnalysis#weight()},
 *        then by {@link ConstraintAnalysis#constraintRef()}.
 * @param isSolutionInitialized Whether the solution was fully initialized at the time of analysis.
 *
 * @param <Score_>
 */
public record ScoreAnalysis<Score_ extends Score<Score_>>(@NonNull Score_ score,
        @NonNull Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap,
        boolean isSolutionInitialized) {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Comparator<ConstraintAnalysis<?>> REVERSED_WEIGHT_COMPARATOR =
            Comparator.<ConstraintAnalysis<?>, Score> comparing(ConstraintAnalysis::weight)
                    .reversed();
    private static final Comparator<ConstraintAnalysis<?>> MAP_COMPARATOR =
            REVERSED_WEIGHT_COMPARATOR.thenComparing(ConstraintAnalysis::constraintRef);

    static final int DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT = 3;

    /**
     * As defined by {@link #ScoreAnalysis(Score, Map, boolean)},
     * with the final argument set to true.
     */
    public ScoreAnalysis(@NonNull Score_ score, @NonNull Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap) {
        this(score, constraintMap, true);
    }

    public ScoreAnalysis {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(constraintMap, "constraintMap");
        // Ensure consistent order and no external interference.
        constraintMap = Collections.unmodifiableMap(constraintMap.values()
                .stream()
                .sorted(MAP_COMPARATOR)
                .collect(Collectors.toMap(
                        ConstraintAnalysis::constraintRef,
                        Function.identity(),
                        (constraintAnalysis, otherConstraintAnalysis) -> constraintAnalysis,
                        LinkedHashMap::new)));
    }

    /**
     * Performs a lookup on {@link #constraintMap()}.
     * Equivalent to {@code constraintMap().get(constraintRef)}.
     *
     * @return null if no constraint matches of such constraint are present
     */
    public @Nullable ConstraintAnalysis<Score_> getConstraintAnalysis(@NonNull ConstraintRef constraintRef) {
        return constraintMap.get(constraintRef);
    }

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}
     * where the arguments are first composed into a singular constraint ID.
     *
     * @return null if no constraint matches of such constraint are present
     * @deprecated Use {@link #getConstraintAnalysis(String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    public @Nullable ConstraintAnalysis<Score_> getConstraintAnalysis(@NonNull String constraintPackage,
            @NonNull String constraintName) {
        return getConstraintAnalysis(ConstraintRef.of(constraintPackage, constraintName));
    }

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}.
     *
     * @return null if no constraint matches of such constraint are present
     * @throws IllegalStateException if multiple constraints with the same name are present,
     *         which is possible if they are in different constraint packages.
     *         Constraint packages are deprecated, we recommend avoiding them and instead naming constraints uniquely.
     *         If you must use constraint packages, see {@link #getConstraintAnalysis(String, String)}
     *         (also deprecated) and reach out to us to discuss your use case.
     */
    public @Nullable ConstraintAnalysis<Score_> getConstraintAnalysis(@NonNull String constraintName) {
        var constraintAnalysisList = constraintMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().constraintName().equals(constraintName))
                .map(Map.Entry::getValue)
                .toList();
        return switch (constraintAnalysisList.size()) {
            case 0 -> null;
            case 1 -> constraintAnalysisList.get(0);
            default -> throw new IllegalStateException("""
                    Multiple constraints with the same name (%s) are present in the score analysis.
                    This may be caused by the use of multiple constraint packages, a deprecated feature.
                    Please avoid using constraint packages and keep constraint names unique."""
                    .formatted(constraintName));
        };
    }

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
    public @NonNull ScoreAnalysis<Score_> diff(@NonNull ScoreAnalysis<Score_> other) {
        var result = Stream.concat(constraintMap.keySet().stream(),
                other.constraintMap.keySet().stream())
                .distinct()
                .flatMap(constraintRef -> {
                    var constraintAnalysis = getConstraintAnalysis(constraintRef);
                    var otherConstraintAnalysis = other.getConstraintAnalysis(constraintRef);
                    var diff = ConstraintAnalysis.diff(constraintRef, constraintAnalysis, otherConstraintAnalysis);
                    // The following code implements logic to decide which information the user needs to see,
                    // and which is information we can safely discard.
                    // This is done so that the diff (which is likely to be serialized into JSON) is not bloated.
                    if (!diff.weight().isZero() || !diff.score().isZero()) { // Guaranteed change.
                        return Stream.of(diff);
                    }
                    // Figuring out whether constraint matches changed is tricky.
                    // Can't use constraint weight; weight diff on the same constraint is zero if weight unchanged.
                    // Can't use matchCount; matchCount diff can be zero if one match was added and another removed.
                    // To detect if the constraint matches changed, we use the actual match diff.
                    if (diff.matches() == null) {
                        // If it is null, either justifications are disabled,
                        // or constraint matching is disabled altogether.
                        // This means we don't have enough information to make smarter decisions.
                        if (diff.matchCount() == 0) {
                            // Returning this makes no practical sense.
                            // The result would be constraint name + zero weight + zero score + zero match count.
                            return Stream.empty();
                        } else {
                            return Stream.of(diff);
                        }
                    } else if (!diff.matches().isEmpty()) {
                        // We actually have constraint matches, and they are meaningfully different.
                        return Stream.of(diff);
                    } else {
                        // This will be empty only if all matches are exactly the same.
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toMap(
                        ConstraintAnalysis::constraintRef,
                        Function.identity(),
                        (constraintRef, otherConstraintRef) -> constraintRef,
                        HashMap::new));
        return new ScoreAnalysis<>(score.subtract(other.score()), result, isSolutionInitialized);
    }

    /**
     * Returns individual {@link ConstraintAnalysis} instances that make up this {@link ScoreAnalysis}.
     *
     * @return equivalent to {@code constraintMap().values()}
     */
    public Collection<ConstraintAnalysis<Score_>> constraintAnalyses() {
        return constraintMap.values();
    }

    /**
     * Returns a diagnostic text that explains the solution through the {@link ConstraintAnalysis} API to identify which
     * constraints cause that score quality.
     * The string is built fresh every time the method is called.
     * <p>
     * In case of an {@link Score#isFeasible() infeasible} solution, this can help diagnose the cause of that.
     *
     * <p>
     * Do not parse the return value, its format may change without warning.
     * Instead, provide this information in a UI or a service,
     * use {@link ScoreAnalysis#constraintAnalyses()}
     * and convert those into a domain-specific API.
     */
    @SuppressWarnings("java:S3457")
    public @NonNull String summarize() {
        StringBuilder summary = new StringBuilder();
        summary.append("""
                Explanation of score (%s):
                    Constraint matches:
                """.formatted(score));
        Comparator<ConstraintAnalysis<Score_>> constraintsScoreComparator = comparing(ConstraintAnalysis::score);
        Comparator<MatchAnalysis<Score_>> matchScoreComparator = comparing(MatchAnalysis::score);

        constraintAnalyses().stream()
                .sorted(constraintsScoreComparator)
                .forEach(constraint -> {
                    var matches = constraint.matches();
                    if (matches == null) {
                        throw new IllegalArgumentException("""
                                The constraint matches must be non-null.
                                Maybe use ScoreAnalysisFetchPolicy.FETCH_ALL to request the score analysis
                                """);
                    }
                    if (matches.isEmpty()) {
                        summary.append(
                                "%8s%s: constraint (%s) has no matches.\n".formatted(" ", constraint.score().toShortString(),
                                        constraint.constraintRef().constraintName()));
                    } else {
                        summary.append(
                                "%8s%s: constraint (%s) has %s matches:\n".formatted(" ", constraint.score().toShortString(),
                                        constraint.constraintRef().constraintName(), matches.size()));
                    }
                    matches.stream()
                            .sorted(matchScoreComparator)
                            .limit(DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT)
                            .forEach(match -> summary
                                    .append("%12s%s: justified with (%s)\n".formatted(" ", match.score().toShortString(),
                                            match.justification())));
                    if (matches.size() > DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT) {
                        summary.append("%12s%s\n".formatted(" ", "..."));
                    }
                });

        return summary.toString();
    }

    @Override
    public String toString() {
        return "Score analysis of score %s with %d constraints.".formatted(score, constraintMap.size());
    }
}
