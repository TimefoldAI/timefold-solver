package ai.timefold.solver.core.api.score.analysis;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;

import org.jspecify.annotations.NonNull;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 */
public record MatchAnalysis<Score_ extends Score<Score_>>(@NonNull ConstraintRef constraintRef, @NonNull Score_ score,
        @NonNull ConstraintJustification justification) implements Comparable<MatchAnalysis<Score_>> {

    public MatchAnalysis {
        Objects.requireNonNull(constraintRef);
        Objects.requireNonNull(score);
        // Null justification is impossible;
        // if the fetch policy doesn't requre match analysis, the code shouldn't even get here.
        Objects.requireNonNull(justification, () -> """
                Impossible state: Received a null justification.
                Maybe check your %s's justifyWith() implementation for that constraint?"""
                .formatted(ConstraintProvider.class));
    }

    MatchAnalysis<Score_> negate() {
        return new MatchAnalysis<>(constraintRef, score.negate(), justification);
    }

    @Override
    public int compareTo(MatchAnalysis<Score_> other) {
        int constraintRefComparison = this.constraintRef.compareTo(other.constraintRef);
        if (constraintRefComparison != 0) {
            return constraintRefComparison;
        }
        int scoreComparison = this.score.compareTo(other.score);
        if (scoreComparison != 0) {
            return scoreComparison;
        } else {
            if (this.justification instanceof Comparable comparableJustification
                    && other.justification instanceof Comparable otherComparableJustification) {
                return comparableJustification.compareTo(otherComparableJustification);
            } else {
                return 0;
            }
        }
    }
}
