package ai.timefold.solver.core.testutil;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.event.BestSolutionChangedEvent;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhase;
import ai.timefold.solver.core.impl.phase.custom.DefaultCustomPhase;
import ai.timefold.solver.core.impl.solver.event.DefaultBestSolutionChangedEvent;

import org.jspecify.annotations.NonNull;

/**
 * Exists to avoid storing each best solution event's solution.
 */
public record BestScoreChangedEvent<Score_ extends Score<@NonNull Score_>>(Score_ newScore, boolean isInitialized,
        String eventId) {
    @SuppressWarnings("unchecked")
    public BestScoreChangedEvent(BestSolutionChangedEvent<?> bestSolutionChangedEvent) {
        this((Score_) bestSolutionChangedEvent.getNewBestScore(), bestSolutionChangedEvent.isNewBestSolutionInitialized(),
                bestSolutionChangedEvent.getProducerId());
    }

    public BestScoreChangedEvent<Score_> uninitialized() {
        return new BestScoreChangedEvent<>(newScore, false, eventId);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> solvingStarted(Score_ newScore) {
        return new BestScoreChangedEvent<>(newScore, true, DefaultBestSolutionChangedEvent.SOLVING_STARTED_EVENT_ID);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> problemChange(Score_ newScore) {
        return new BestScoreChangedEvent<>(newScore, true, DefaultBestSolutionChangedEvent.PROBLEM_CHANGE_EVENT_ID);
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_>
            constructionHeuristic(Score_ newScore, int index) {
        return new BestScoreChangedEvent<>(newScore, true,
                "%s (%d)".formatted(DefaultConstructionHeuristicPhase.CONSTRUCTION_HEURISTICS_STRING, index));
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> custom(Score_ newScore,
            int index) {
        return new BestScoreChangedEvent<>(newScore, true,
                "%s (%d)".formatted(DefaultCustomPhase.CUSTOM_STRING, index));
    }

    public static <Score_ extends Score<@NonNull Score_>> BestScoreChangedEvent<Score_> localSearch(Score_ newScore,
            int index) {
        return new BestScoreChangedEvent<>(newScore, true,
                "%s (%d)".formatted(DefaultLocalSearchPhase.LOCAL_SEARCH_STRING, index));
    }
}
