package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.jspecify.annotations.Nullable;

public record HybridGeneticSearchConfiguration<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        int populationSize, int generationSize, int eliteSolutionSize, int populationRestartCount,
        ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy, Phase<Solution_> localSearchPhase,
        @Nullable Phase<Solution_> refinementPhase, CrossoverStrategy<Solution_, Score_> crossoverStrategy,
        IndividualBuilder<Solution_, Score_> individualBuilder,
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager, PhaseTermination<Solution_> phaseTermination,
        BestSolutionRecaller<Solution_> bestSolutionRecaller) {

    static <Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            HybridGeneticSearchConfiguration<Solution_, Score_, State_>
            of(HybridGeneticSearchDecider.Builder<Solution_, Score_, State_, ?> builder) {
        return new HybridGeneticSearchConfiguration<>(builder.populationSize,
                builder.generationSize, builder.eliteSolutionSize, builder.populationRestartCount,
                Objects.requireNonNull(builder.constructionIndividualStrategy),
                Objects.requireNonNull(builder.localSearchPhase),
                builder.refinementPhase, Objects.requireNonNull(builder.crossoverStrategy),
                Objects.requireNonNull(builder.individualBuilder), Objects.requireNonNull(builder.solutionStateManager),
                Objects.requireNonNull(builder.phaseTermination),
                Objects.requireNonNull(builder.bestSolutionRecaller));
    }
}
