package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;

public record HybridGeneticSearchWorkerContext<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy, Phase<Solution_> localSearchPhase,
        Phase<Solution_> refinementPhase, CrossoverStrategy<Solution_, Score_> crossoverStrategy,
        IndividualBuilder<Solution_, Score_> individualBuilder,
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager) {
}
