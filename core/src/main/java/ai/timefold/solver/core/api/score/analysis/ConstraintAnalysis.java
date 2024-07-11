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

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param constraintRef never null
 * @param weight never null
 * @param score never null
 * @param matches null if analysis not available;
 *        empty if constraint has no matches, but still non-zero constraint weight;
 *        non-empty if constraint has matches.
 *        This is a {@link List} to simplify access to individual elements,
 *        but it contains no duplicates just like {@link HashSet} wouldn't.
 */
public record ConstraintAnalysis<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ weight,
        Score_ score, List<MatchAnalysis<Score_>> matches) {

    static <Score_ extends Score<Score_>> ConstraintAnalysis<Score_> of(ConstraintRef constraintRef, Score_ constraintWeight,
            Score_ score) {
        return new ConstraintAnalysis<>(constraintRef, constraintWeight, score, null);
    }

    public ConstraintAnalysis {
        Objects.requireNonNull(constraintRef);
        if (weight == null) {
            /*
             * Only possible in ConstraintMatchAwareIncrementalScoreCalculator and/or tests.
             * Easy doesn't support constraint analysis at all.
             * CS always provides constraint weights.
             */
            throw new IllegalArgumentException("""
                    The constraint weight must be non-null.
                    Maybe use a non-deprecated %s constructor in your %s implementation?
                    """
                    .stripTrailing()
                    .formatted(DefaultConstraintMatchTotal.class.getSimpleName(),
                            ConstraintMatchAwareIncrementalScoreCalculator.class.getSimpleName()));
        }
        Objects.requireNonNull(score);
    }

    /**
     * Return the match count of the constraint.
     *
     * @throws IllegalStateException if the {@link ConstraintAnalysis#matches()} is null
     */
    public int matchCount() {
        if (matches == null) {
            throw new IllegalArgumentException("""
                    The constraint matches must be non-null.
                    Maybe use ScoreAnalysisFetchPolicy.FETCH_ALL to request the score analysis
                    """);
        }
        return matches.size();
    }

    ConstraintAnalysis<Score_> negate() {
        if (matches == null) {
            return ConstraintAnalysis.of(constraintRef, weight.negate(), score.negate());
        } else {
            var negatedMatchAnalyses = matches.stream()
                    .map(MatchAnalysis::negate)
                    .toList();
            return new ConstraintAnalysis<>(constraintRef, weight.negate(), score.negate(), negatedMatchAnalyses);
        }
    }

    static <Score_ extends Score<Score_>> ConstraintAnalysis<Score_> diff(
            ConstraintRef constraintRef, ConstraintAnalysis<Score_> constraintAnalysis,
            ConstraintAnalysis<Score_> otherConstraintAnalysis) {
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
                    "Impossible state: Only one of the score analyses (%s, %s) provided match analyses for a constraint (%s)."
                            .formatted(constraintAnalysis, otherConstraintAnalysis, constraintRef));
        }
        // Compute the diff.
        var constraintWeightDifference = constraintAnalysis.weight().subtract(otherConstraintAnalysis.weight());
        var scoreDifference = constraintAnalysis.score().subtract(otherConstraintAnalysis.score());
        if (matchAnalyses == null) {
            return ConstraintAnalysis.of(constraintRef, constraintWeightDifference, scoreDifference);
        }
        var matchAnalysisMap = mapMatchesToJustifications(matchAnalyses);
        var otherMatchAnalysisMap = mapMatchesToJustifications(otherMatchAnalyses);
        var result = Stream.concat(matchAnalysisMap.keySet().stream(), otherMatchAnalysisMap.keySet().stream())
                .distinct()
                .map(justification -> {
                    var matchAnalysis = matchAnalysisMap.get(justification);
                    var otherMatchAnalysis = otherMatchAnalysisMap.get(justification);
                    if (matchAnalysis == null) {
                        if (otherMatchAnalysis == null) {
                            throw new IllegalStateException(
                                    "Impossible state: none of the match analyses provided for a constraint (%s)."
                                            .formatted(constraintRef));
                        }
                        // No need to compute diff; this match is not present in this score explanation.
                        return otherMatchAnalysis.negate();
                    } else if (otherMatchAnalysis == null) {
                        // No need to compute diff; this match is not present in the other score explanation.
                        return matchAnalysis;
                    } else { // Compute the diff.
                        return new MatchAnalysis<>(constraintRef, matchAnalysis.score().subtract(otherMatchAnalysis.score()),
                                justification);
                    }
                })
                .toList();
        return new ConstraintAnalysis<>(constraintRef, constraintWeightDifference, scoreDifference, result);
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
    public String constraintName() {
        return constraintRef.constraintName();
    }

    /**
     * Returns a diagnostic text that explains part of the score quality through the {@link ConstraintAnalysis} API.
     * The string is built fresh every time the method is called.
     *
     * @return never null
     */
    @SuppressWarnings("java:S3457")
    public String summarize() {
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
            return "(%s at %s, no matches)"
                    .formatted(score, weight);
        } else {
            return "(%s at %s, %s matches)"
                    .formatted(score, weight, matches.size());
        }
    }
}
