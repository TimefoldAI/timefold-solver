package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;

/**
 * Base contract for defining crossover operations.
 */
@FunctionalInterface
@NullMarked
public interface CrossoverStrategy<Solution_, Score_ extends Score<Score_>> {

    CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context);
}
