package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record HybridGeneticSearchWorkerContext<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        double exploratoryRate,
        ConstructionIndividualStrategy<Solution_, Score_> exploratoryConstructionIndividualStrategy,
        ConstructionIndividualStrategy<Solution_, Score_> conservativeConstructionIndividualStrategy,
        CrossoverStrategy<Solution_, Score_> exploratoryCrossoverStrategy,
        CrossoverStrategy<Solution_, Score_> conservativeCrossoverStrategy,
        IndividualBuilder<Solution_, Score_> individualBuilder,
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager) {

    public static <Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>, Solution_>
            HybridGeneticSearchWorkerContext<Solution_, Score_, State_>
            of(double exploratoryRate, HybridGeneticSearchWorkerContext<Solution_, Score_, State_> otherWorkerContext) {
        return new HybridGeneticSearchWorkerContext<>(exploratoryRate,
                otherWorkerContext.exploratoryConstructionIndividualStrategy,
                otherWorkerContext.conservativeConstructionIndividualStrategy, otherWorkerContext.exploratoryCrossoverStrategy,
                otherWorkerContext.conservativeCrossoverStrategy, otherWorkerContext.individualBuilder,
                otherWorkerContext.solutionStateManager);
    }
}
