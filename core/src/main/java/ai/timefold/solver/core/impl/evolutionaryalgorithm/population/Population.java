package ai.timefold.solver.core.impl.evolutionaryalgorithm.population;

import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the population of candidate solutions maintained by an evolutionary algorithm.
 * <p>
 * A population maintains a collection of {@link Individual individuals} and is responsible for
 * three core operations that drive the evolutionary process:
 * <ul>
 * <li><b>Insertion</b> — adding newly produced offspring, optionally triggering survival
 * selection to keep the population within its limit.</li>
 * <li><b>Selection</b> — choosing individuals to act as parents for producing offsprings.</li>
 * <li><b>Restart</b> — periodically clearing stale diversity and seeding fresh individuals
 * while preserving elite solutions.</li>
 * </ul>
 * 
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 * 
 * @see DefaultPopulation
 * @see EvolutionaryDecider
 */

@NullMarked
public interface Population<Solution_, Score_ extends Score<Score_>> {

    /**
     * Add a new individual to the population
     * 
     * @param individual the individual to be added
     */
    void addIndividual(Individual<Solution_, Score_> individual, boolean enableSurvivalSelection);

    /**
     * Select an individual from the population.
     * Different strategies, such as binary tournament selection, can be used to choose the individual.
     * 
     * @return an individual from the population.
     */
    Individual<Solution_, Score_> selectIndividual();

    /**
     * Recreate the population with new individuals.
     */
    void restart(List<Individual<Solution_, Score_>> individuals);

    /**
     * @return the current best individual or null otherwise.
     */
    @Nullable
    Individual<Solution_, Score_> getBestIndividual();

    /**
     * @return the size of the population.
     */
    int size();

    /**
     * @return statistics on population evolution during the optimization process.
     */
    PopulationStatistics getStatistics();
}
