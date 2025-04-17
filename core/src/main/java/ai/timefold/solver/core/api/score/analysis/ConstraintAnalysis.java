package ai.timefold.solver.core.api.score.analysis;

import static ai.timefold.solver.core.api.score.analysis.ScoreAnalysis.DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT;
import static java.util.Comparator.comparing;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.calculator.ConstraintMatchAwareIncrementalScoreCalculator;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.score.constraint.DefaultConstraintMatchTotal;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param matches null if analysis not available;
 *        empty if constraint has no matches, but still non-zero constraint weight;
 *        non-empty if constraint has matches.
 *        This is a {@link List} to simplify access to individual elements,
 *        but it contains no duplicates just like {@link HashSet} wouldn't.
 * @param matchCount
 *        <ul>
 *        <li>For regular constraint analysis:
 *        -1 if analysis not available,
 *        0 if constraint has no matches,
 *        positive if constraint has matches.
 *        Equal to the size of the {@link #matches} list.</li>
 *        <li>For a {@link ScoreAnalysis#diff(ScoreAnalysis) diff of constraint analyses}:
 *        positive if the constraint has more matches in the new analysis,
 *        zero if the number of matches is the same in both,
 *        negative otherwise.
 *        Need not be equal to the size of the {@link #matches} list.</li>
 *        </ul>
 */
public record ConstraintAnalysis<Score_ extends Score<Score_>>(@NonNull ConstraintRef constraintRef, @NonNull Score_ weight,
        @NonNull Score_ score, @Nullable List<MatchAnalysis<Score_>> matches, int matchCount) {

    public ConstraintAnalysis(@NonNull ConstraintRef constraintRef, @NonNull Score_ weight, @NonNull Score_ score,
            @Nullable List<MatchAnalysis<Score_>> matches) {
        this(constraintRef, weight, score, matches, matches == null ? -1 : matches.size());
    }

    public ConstraintAnalysis {
        Objects.requireNonNull(constraintRef);
        /*
         * Null only possible in ConstraintMatchAwareIncrementalScoreCalculator and/or tests.
         * Easy doesn't support constraint analysis at all.
         * CS always provides constraint weights.
         */
        Objects.requireNonNull(weight, () -> """
                The constraint weight must be non-null.
                Maybe use a non-deprecated %s constructor in your %s implementation?"""
                .formatted(DefaultConstraintMatchTotal.class.getSimpleName(),
                        ConstraintMatchAwareIncrementalScoreCalculator.class.getSimpleName()));
        Objects.requireNonNull(score);
    }

    @NonNull
    ConstraintAnalysis<Score_> negate() {
        // Only used to compute diff; use semantics for non-diff.
        // A negative match count is only allowed within these semantics when matches == null.
        if (matches == null) {
            // At this point, matchCount is already negative, as matches == null.
            return new ConstraintAnalysis<>(constraintRef, weight.negate(), score.negate(), null, matchCount);
        } else {
            // Within these semantics, match count == list size.
            var negatedMatchAnalysesList = matches.stream()
                    .map(MatchAnalysis::negate)
                    .toList();
            return new ConstraintAnalysis<>(constraintRef, weight.negate(), score.negate(), negatedMatchAnalysesList,
                    matchCount);
        }
    }

    static <Score_ extends Score<Score_>> @NonNull ConstraintAnalysis<Score_> diff(
            @NonNull ConstraintRef constraintRef, @Nullable ConstraintAnalysis<Score_> constraintAnalysis,
            @Nullable ConstraintAnalysis<Score_> otherConstraintAnalysis) {
        if (constraintAnalysis == null) {
            if (otherConstraintAnalysis == null) {
                throw new IllegalStateException(
                        "Impossible state: none of the score explanations provided constraint matches for a constraint (%s)."
                                .formatted(constraintRef));
            }
            // No need to compute diff; this constraint is not present in this score explanation.
            return otherConstraintAnalysis.negate();
        } else if (otherConstraintAnalysis == null) {
            // No need to compute diff; this constraint is not present in the other score explanation.
            return constraintAnalysis;
        }
        var matchAnalyses = constraintAnalysis.matches();
        var otherMatchAnalyses = otherConstraintAnalysis.matches();
        if ((matchAnalyses == null && otherMatchAnalyses != null) || (matchAnalyses != null && otherMatchAnalyses == null)) {
            throw new IllegalStateException(
                    "Impossible state: One of the score analyses (%s, %s) provided no match analysis for a constraint (%s)."
                            .formatted(constraintAnalysis, otherConstraintAnalysis, constraintRef));
        }
        // Compute the diff.
        var constraintWeightDifference = constraintAnalysis.weight().subtract(otherConstraintAnalysis.weight());
        var scoreDifference = constraintAnalysis.score().subtract(otherConstraintAnalysis.score());
        if (matchAnalyses == null) {
            var leftHasMatchCount = hasMatchCount(constraintAnalysis);
            var rightHasMatchCount = hasMatchCount(otherConstraintAnalysis);
            if ((!leftHasMatchCount && rightHasMatchCount) || (leftHasMatchCount && !rightHasMatchCount)) {
                throw new IllegalStateException(
                        "Impossible state: One of the score analyses (%s, %s) provided no match count for a constraint (%s)."
                                .formatted(constraintAnalysis, otherConstraintAnalysis, constraintRef));
            }
            return new ConstraintAnalysis<>(constraintRef, constraintWeightDifference, scoreDifference, null,
                    getMatchCount(constraintAnalysis, otherConstraintAnalysis));
        }
        var matchAnalysisMap = mapMatchesToJustifications(matchAnalyses);
        var otherMatchAnalysisMap = mapMatchesToJustifications(otherMatchAnalyses);
        var matchAnalysesList = Stream.concat(matchAnalysisMap.keySet().stream(), otherMatchAnalysisMap.keySet().stream())
                .distinct()
                .flatMap(justification -> {
                    var matchAnalysis = matchAnalysisMap.get(justification);
                    var otherMatchAnalysis = otherMatchAnalysisMap.get(justification);
                    if (matchAnalysis == null) {
                        if (otherMatchAnalysis == null) {
                            throw new IllegalStateException(
                                    "Impossible state: none of the match analyses provided for a constraint (%s)."
                                            .formatted(constraintRef));
                        }
                        // No need to compute diff; this match is not present in this score explanation.
                        return Stream.of(otherMatchAnalysis.negate());
                    } else if (otherMatchAnalysis == null) {
                        // No need to compute diff; this match is not present in the other score explanation.
                        return Stream.of(matchAnalysis);
                    } else if (!matchAnalysis.equals(otherMatchAnalysis)) { // Compute the diff.
                        return Stream.of(new MatchAnalysis<>(constraintRef,
                                matchAnalysis.score().subtract(otherMatchAnalysis.score()), justification));
                    } else { // There is no difference; skip entirely.
                        return Stream.empty();
                    }
                }).toList();
        return new ConstraintAnalysis<>(constraintRef, constraintWeightDifference, scoreDifference, matchAnalysesList,
                getMatchCount(constraintAnalysis, otherConstraintAnalysis));
    }

    private static boolean hasMatchCount(ConstraintAnalysis<?> analysis) {
        return analysis.matchCount >= 0;
    }

    private static int getMatchCount(ConstraintAnalysis<?> analysis, ConstraintAnalysis<?> otherAnalysis) {
        return analysis.matchCount() - otherAnalysis.matchCount();
    }

    private static <Score_ extends Score<Score_>> Map<ConstraintJustification, MatchAnalysis<Score_>>
            mapMatchesToJustifications(List<MatchAnalysis<Score_>> matchAnalyses) {
        Map<ConstraintJustification, MatchAnalysis<Score_>> matchAnalysisMap =
                CollectionUtils.newLinkedHashMap(matchAnalyses.size());
        for (var matchAnalysis : matchAnalyses) {
            var previous = matchAnalysisMap.put(matchAnalysis.justification(), matchAnalysis);
            if (previous != null) {
                // Match analysis for the same justification should have been merged already.
                throw new IllegalStateException(
                        "Impossible state: multiple constraint matches (%s, %s) have the same justification (%s)."
                                .formatted(previous, matchAnalysis, matchAnalysis.justification()));
            }
        }
        return matchAnalysisMap;
    }

    /**
     * Return package name of the constraint that this analysis is for.
     *
     * @return equal to {@code constraintRef.packageName()}
     * @deprecated Do not rely on constraint package in user code.
     */
    @Deprecated(forRemoval = true, since = "1.13.0")
    public String constraintPackage() {
        return constraintRef.packageName();
    }

    /**
     * Return name of the constraint that this analysis is for.
     *
     * @return equal to {@code constraintRef.constraintName()}
     */
    public @NonNull String constraintName() {
        return constraintRef.constraintName();
    }

    /**
     * Returns a diagnostic text that explains part of the score quality through the {@link ConstraintAnalysis} API.
     * The string is built fresh every time the method is called.
     */
    @SuppressWarnings("java:S3457")
    public @NonNull String summarize() {
        var summary = new StringBuilder();
        summary.append("""
                Explanation of score (%s):
                    Constraint matches:
                """.formatted(score));
        Comparator<MatchAnalysis<Score_>> matchScoreComparator = comparing(MatchAnalysis::score);

        var constraintMatches = matches();
        if (constraintMatches == null) {
            throw new IllegalArgumentException("""
                    The constraint matches must be non-null.
                    Maybe use ScoreAnalysisFetchPolicy.FETCH_ALL to request the score analysis
                    """);
        }
        if (constraintMatches.isEmpty()) {
            summary.append(
                    "%8s%s: constraint (%s) has no matches.\n".formatted(" ", score().toShortString(),
                            constraintRef().constraintName()));
        } else {
            summary.append("%8s%s: constraint (%s) has %s matches:\n".formatted(" ", score().toShortString(),
                    constraintRef().constraintName(), constraintMatches.size()));
        }
        constraintMatches.stream()
                .sorted(matchScoreComparator)
                .limit(DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT)
                .forEach(match -> summary.append("%12S%s: justified with (%s)\n".formatted(" ", match.score().toShortString(),
                        match.justification())));
        if (constraintMatches.size() > DEFAULT_SUMMARY_CONSTRAINT_MATCH_LIMIT) {
            summary.append("%12s%s\n".formatted(" ", "..."));
        }

        return summary.toString();
    }

    @Override
    public String toString() {
        if (matches == null) {
            if (matchCount == -1) {
                return "(%s at %s, constraint matching disabled)"
                        .formatted(score, weight);
            } else {
                return "(%s at %s, %d matches, justifications disabled)"
                        .formatted(score, weight, matchCount);
            }
        } else {
            return "(%s at %s, %d matches with justifications)"
                    .formatted(score, weight, matches.size());
        }
    }
}
