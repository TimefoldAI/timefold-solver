package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;

public record CrossoverContext<Solution_, Score_ extends Score<Score_>>(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope,
        Individual<Solution_, Score_> firstIndividual, Individual<Solution_, Score_> secondIndividual) {
}
