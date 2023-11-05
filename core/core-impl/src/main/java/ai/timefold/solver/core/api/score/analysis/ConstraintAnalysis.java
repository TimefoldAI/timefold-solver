package ai.timefold.solver.core.api.score.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.impl.util.CollectionUtils;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param constraintRef never null
 * @param score never null
 * @param matches null if analysis not available;
 *        empty if constraint has no matches, but still non-zero constraint weight;
 *        non-empty if constraint has matches.
 *        This is a {@link List} to simplify access to individual elements,
 *        but it contains no duplicates just like {@link HashSet} wouldn't.
 */
public record ConstraintAnalysis<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ score,
        List<MatchAnalysis<Score_>> matches) {

    static <Score_ extends Score<Score_>> ConstraintAnalysis<Score_> of(ConstraintRef constraintRef, Score_ score) {
        return new ConstraintAnalysis<>(constraintRef, score, null);
    }

    public ConstraintAnalysis {
        Objects.requireNonNull(score);
    }

    ConstraintAnalysis<Score_> negate() {
        if (matches == null) {
            return ConstraintAnalysis.of(constraintRef, score.negate());
        } else {
            var negatedMatchAnalyses = matches.stream()
                    .map(MatchAnalysis::negate)
                    .toList();
            return new ConstraintAnalysis<>(constraintRef, score.negate(), negatedMatchAnalyses);
        }
    }

    static <Score_ extends Score<Score_>> ConstraintAnalysis<Score_> diff(
            ConstraintRef constraintRef, ConstraintAnalysis<Score_> constraintAnalysis,
            ConstraintAnalysis<Score_> otherConstraintAnalysis) {
        if (constraintAnalysis == null) {
            if (otherConstraintAnalysis == null) {
                throw new IllegalStateException("""
                        Impossible state: none of the score explanations provided constraint matches for a constraint (%s).
                        """.formatted(constraintRef));
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
            throw new IllegalStateException("""
                    Impossible state: Only one of the score analyses (%s, %s) provided match analyses for a constraint (%s)."""
                    .formatted(constraintAnalysis, otherConstraintAnalysis, constraintRef));
        }
        // Compute the diff.
        var scoreDifference = constraintAnalysis.score().subtract(otherConstraintAnalysis.score());
        if (matchAnalyses == null) {
            return ConstraintAnalysis.of(constraintRef, scoreDifference);
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
                            throw new IllegalStateException("""
                                    Impossible state: none of the match analyses provided for a constraint (%s).
                                    """.formatted(constraintRef));
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
                .collect(Collectors.toList());
        return new ConstraintAnalysis<>(constraintRef, scoreDifference, result);
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

    @Override
    public String toString() {
        if (matches == null) {
            return "(" + score + ", no match analysis)";
        } else {
            return "(" + score + ", " + matches.size() + " matches)";
        }
    }
}
