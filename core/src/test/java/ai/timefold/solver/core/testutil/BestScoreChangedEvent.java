package ai.timefold.solver.core.testutil;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.api.solver.phase.PhaseType;
import ai.timefold.solver.core.impl.phase.event.DefaultPhaseEventProducerId;
import ai.timefold.solver.core.impl.solver.event.SolveEventProducerId;

import org.jspecify.annotations.NonNull;

/**
 * Exists to avoid storing each best solution event's solution.
 */
public record BestScoreChangedEvent<Score_ extends Score<@NonNull Score_>>(Score_ newScore, boolean isInitialized,
        EventProducerId eventProducerId) {
    @SuppressWarnings("unchecked")
    public BestScoreChangedEvent(BestSolutionChangedEvent<?> bestSolutionChangedEvent) {
        this((Score_) bestSolutionChangedEvent.getNewBestScore(), bestSolutionChangedEvent.isNewBestSolutionInitialized(),
                bestSolutionChangedEvent.getProducerId());
    }

    public BestScoreChangedEvent<Score_> uninitialized() {
        return new BestScoreChangedEvent<>(newScore, false, eventProducerId);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> solvingStarted(Score_ newScore) {
        return new BestScoreChangedEvent<>(newScore, true, SolveEventProducerId.SOLVING_STARTED);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> problemChange(Score_ newScore) {
        return new BestScoreChangedEvent<>(newScore, true, SolveEventProducerId.PROBLEM_CHANGE);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_>
            constructionHeuristic(Score_ newScore, int index) {
        return new BestScoreChangedEvent<>(newScore, true,
                DefaultPhaseEventProducerId.of(PhaseType.CONSTRUCTION_HEURISTIC, index));
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> custom(Score_ newScore,
            int index) {
        return new BestScoreChangedEvent<>(newScore, true, DefaultPhaseEventProducerId.of(PhaseType.CUSTOM_PHASE, index));
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> localSearch(Score_ newScore,
            int index) {
        return new BestScoreChangedEvent<>(newScore, true, DefaultPhaseEventProducerId.of(PhaseType.LOCAL_SEARCH, index));
    }
}
