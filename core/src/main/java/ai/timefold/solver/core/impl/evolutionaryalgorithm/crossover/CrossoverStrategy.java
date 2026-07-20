package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.phase.Phase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base contract for defining crossover operations.
 */
@NullMarked
public interface CrossoverStrategy<Solution_, Score_ extends Score<Score_>> {

    CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context);

    Phase<Solution_> getLocalSearchPhase();

    @Nullable
    Phase<Solution_> getRefinementPhase();
}
