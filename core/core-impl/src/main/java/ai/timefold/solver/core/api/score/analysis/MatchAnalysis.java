package ai.timefold.solver.core.api.score.analysis;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;

/**
 * Note: Users should never create instances of this type directly.
 * It is available transitively via {@link SolutionManager#analyze(Object)}.
 *
 * @param <Score_>
 * @param score never null
 * @param justification never null
 */
public record MatchAnalysis<Score_ extends Score<Score_>>(Score_ score, ConstraintJustification justification) {

    public MatchAnalysis {
        Objects.requireNonNull(score);
        Objects.requireNonNull(justification, """
                Received a null justification.
                Maybe check your %s's justifyWith() implementation for that constraint?"""
                .formatted(ConstraintProvider.class));
    }

    MatchAnalysis<Score_> negate() {
        return new MatchAnalysis<>(score.negate(), justification);
    }

}
