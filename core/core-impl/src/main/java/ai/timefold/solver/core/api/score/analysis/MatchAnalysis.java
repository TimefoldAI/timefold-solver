package ai.timefold.solver.core.api.score.analysis;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param constraintRef never null
 * @param score never null
 * @param justification never null
 */
public record MatchAnalysis<Score_ extends Score<Score_>>(ConstraintRef constraintRef, Score_ score,
        ConstraintJustification justification) implements Comparable<MatchAnalysis<Score_>> {

    public MatchAnalysis {
        Objects.requireNonNull(constraintRef);
        Objects.requireNonNull(score);
        Objects.requireNonNull(justification, """
                Received a null justification.
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
            if (this.justification instanceof Comparable && other.justification instanceof Comparable) {
                return ((Comparable) this.justification).compareTo(other.justification);
            } else {
                return 0;
            }
        }
    }
}
