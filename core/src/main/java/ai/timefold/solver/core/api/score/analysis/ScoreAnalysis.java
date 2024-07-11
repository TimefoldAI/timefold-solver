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
import ai.timefold.solver.core.api.solver.SolutionManager;

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
 * @param score never null
 * @param constraintMap never null;
 *        for each constraint identified by its {@link Constraint#getConstraintRef()},
 *        the {@link ConstraintAnalysis} that describes the impact of that constraint on the overall score.
 *        Constraints are present even if they have no matches, unless their weight is zero;
 *        zero-weight constraints are not present.
 *        Entries in the map have a stable iteration order; items are ordered first by {@link ConstraintAnalysis#weight()},
 *        then by {@link ConstraintAnalysis#constraintRef()}.
 *
 * @param <Score_>
 */
public record ScoreAnalysis<Score_ extends Score<Score_>>(Score_ score,
        Map<ConstraintRef, ConstraintAnalysis<Score_>> constraintMap) {

    static final int DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT = 3;

    public ScoreAnalysis {
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(constraintMap, "constraintMap");
        // Ensure consistent order and no external interference.
        var comparator = Comparator.<ConstraintAnalysis<Score_>, Score_> comparing(ConstraintAnalysis::weight)
                .reversed()
                .thenComparing(ConstraintAnalysis::constraintRef);
        constraintMap = Collections.unmodifiableMap(constraintMap.values()
                .stream()
                .sorted(comparator)
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
     * @param constraintRef never null
     * @return null if no constraint matches of such constraint are present
     */
    public ConstraintAnalysis<Score_> getConstraintAnalysis(ConstraintRef constraintRef) {
        return constraintMap.get(constraintRef);
    }

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}
     * where the arguments are first composed into a singular constraint ID.
     *
     * @param constraintPackage never null
     * @param constraintName never null
     * @return null if no constraint matches of such constraint are present
     * @deprecated Use {@link #getConstraintAnalysis(String)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    public ConstraintAnalysis<Score_> getConstraintAnalysis(String constraintPackage, String constraintName) {
        return getConstraintAnalysis(ConstraintRef.of(constraintPackage, constraintName));
    }

    /**
     * As defined by {@link #getConstraintAnalysis(ConstraintRef)}.
     *
     * @param constraintName never null
     * @return null if no constraint matches of such constraint are present
     * @throws IllegalStateException if multiple constraints with the same name are present,
     *         which is possible if they are in different constraint packages.
     *         Constraint packages are deprecated, we recommend avoiding them and instead naming constraints uniquely.
     *         If you must use constraint packages, see {@link #getConstraintAnalysis(String, String)}
     *         (also deprecated) and reach out to us to discuss your use case.
     */
    public ConstraintAnalysis<Score_> getConstraintAnalysis(String constraintName) {
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
     * @param other never null
     * @return never null
     */
    public ScoreAnalysis<Score_> diff(ScoreAnalysis<Score_> other) {
        var result = Stream.concat(constraintMap.keySet().stream(),
                other.constraintMap.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        constraintRef -> {
                            var constraintAnalysis = getConstraintAnalysis(constraintRef);
                            var otherConstraintAnalysis = other.getConstraintAnalysis(constraintRef);
                            return ConstraintAnalysis.diff(constraintRef, constraintAnalysis, otherConstraintAnalysis);
                        },
                        (constraintRef, otherConstraintRef) -> constraintRef,
                        HashMap::new));
        return new ScoreAnalysis<>(score.subtract(other.score()), result);
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
     *
     * @return never null
     */
    @SuppressWarnings("java:S3457")
    public String summarize() {
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

    public boolean isSolutionInitialized() {
        return score().isSolutionInitialized();
    }

    @Override
    public String toString() {
        return "Score analysis of score %s with %d constraints.".formatted(score, constraintMap.size());
    }
}
