package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Basic contract for implementing evolutionary algorithms.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
public interface EvolutionaryDecider<Solution_, Score_ extends Score<Score_>> {

    /**
     * Returns an empty population that will be filled by later actions.
     * 
     * @param phaseScope the phase scope.
     * 
     * @return A fresh instance of the population without any individuals.
     */
    Population<Solution_, Score_> emptyPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    /**
     * Creates new individuals and load population, serving as a foundation for the subsequent generations.
     */
    void loadPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope);

    /**
     * The population is updated using this method, and the logic used will vary according to the evolutionary strategy.
     * This method will manage all operations related to the evolutionary strategy
     * (genetic search, hybrid genetic search, genetic programming, etc.),
     * such as parent selection, recombination, mutation, and survival selection.
     *
     * @param stepScope the step scope.
     */
    void evolvePopulation(EvolutionaryAlgorithmStepScope<Solution_> stepScope);

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    void solvingStarted(SolverScope<Solution_> solverScope);

    void solvingEnded(SolverScope<Solution_> solverScope);

    void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> abstractPhaseScope);

    void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> abstractPhaseScope);

    void stepStarted(EvolutionaryAlgorithmStepScope<Solution_> abstractStepScope);

    void stepEnded(EvolutionaryAlgorithmStepScope<Solution_> abstractStepScope);
}
