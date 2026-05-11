package ai.timefold.solver.core.impl.evolutionaryalgorithm;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.Phase;

/**
 * A {@link EvolutionaryAlgorithmPhase} is a {@link Phase} which applies an evolutionary algorithm,
 * such as Genetic Algorithm, Hybrid Genetic Search, Genetic Programming, Particle Swarm Optimization, etc.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface EvolutionaryAlgorithmPhase<Solution_> extends Phase<Solution_> {

}
