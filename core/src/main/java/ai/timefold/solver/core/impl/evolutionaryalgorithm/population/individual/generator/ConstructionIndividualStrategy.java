package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;

import org.jspecify.annotations.NullMarked;

/**
 * Base contract for defining individuals' creation operations.
 */
@NullMarked
@FunctionalInterface
public interface ConstructionIndividualStrategy<Solution_, Score_ extends Score<Score_>> {

    Individual<Solution_, Score_> apply(EvolutionaryAlgorithmStepScope<Solution_> stepScope);
}
