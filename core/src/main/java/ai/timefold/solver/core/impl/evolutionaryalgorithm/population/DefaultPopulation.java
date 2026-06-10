package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;

/**
 * Single-threaded implementation of {@link Population}.
 *
 * @see AbstractPopulation
 */
@NullMarked
public final class DefaultPopulation<Solution_, Score_ extends Score<Score_>>
        extends AbstractPopulation<Solution_, Score_> {

    public DefaultPopulation(int populationSize, int generationSize, int eliteSolutionSize) {
        super(populationSize, generationSize, eliteSolutionSize);
    }
}
