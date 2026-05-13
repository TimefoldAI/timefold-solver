package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution.DefaultBestSolutionUpdater;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.DefaultPopulation;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.thread.ChildThreadType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the Hybrid Genetic Search algorithm, as described in:
 * <em>Hybrid Genetic Search for the CVRP: Open-Source Implementation and SWAP* Neighborhood</em>
 * by Thibaut Vidal.
 * <p>
 * The algorithm has in three phases:
 * <ol>
 * <li><b>Initialize population</b> — loads the population with
 * {@code populationSize × populationSizeMultiplier} individuals generated via
 * construction heuristic followed by local search, without survival selection.</li>
 * <li><b>Evolve population</b> — each step selects two distinct individuals with different scores
 * via binary tournament, applies the crossover strategy to produce an offspring,
 * and adds it to the population with survival selection enabled. Survival selection
 * trims the population back to {@code populationSize} using the biased-fitness
 * criterion (see {@link DefaultPopulation}).</li>
 * <li><b>Restart population</b> — if no best individual has been added for more than
 * {@code populationRestartCount} iterations,
 * the population is cleared except for the {@code eliteSolutionSize} best individuals,
 * and fresh individuals are generated to replace the rest using the same logic as the first phase.</li>
 * </ol>
 * <p>
 * All computation — construction, local search, crossover, and solution-state management —
 * is delegated to {@link HybridGeneticSearchWorker}, which operates on a dedicated
 * score director for isolation from the main solver scope.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 * @param <State_> the solution state type used to save and restore the working solution
 *        during crossover
 */
@NullMarked
public final class HybridGeneticSearchDecider<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
        implements EvolutionaryDecider<Solution_, Score_> {

    private final HybridGeneticSearchConfiguration<Solution_, Score_, State_> configuration;

    @Nullable
    private HybridGeneticSearchWorker<Solution_, Score_, State_> worker = null;

    private long lastBestIter;

    public HybridGeneticSearchDecider(Builder<Solution_, Score_, State_, ?> builder) {
        this.configuration = HybridGeneticSearchConfiguration.of(builder);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Population<Solution_, Score_> emptyPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        return new DefaultPopulation<>(phaseScope.getWorkingRandom(), configuration.populationSize(),
                configuration.generationSize(), configuration.eliteSolutionSize());
    }

    @Override
    public void loadPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        var population = phaseScope.<Score_> getPopulation();
        var nonNullWorker = Objects.requireNonNull(worker);
        while (population.size() < configuration.populationSize()) {
            if (phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                break;
            }
            nonNullWorker.generateIndividual(phaseScope, individual -> population.addIndividual(individual, false));
            // Each individual produced contributes one additional movement. 
            // Therefore, the movement speed will be the number of produced individuals divided by time.
            phaseScope.getSolverScope().addMoveEvaluationCount(1L);
        }
    }

    @Override
    public void evolvePopulation(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var population = phaseScope.<Score_> getPopulation();
        var firstIndividual = population.selectIndividual();
        var secondIndividual = population.selectIndividual();
        var bailout = population.size();
        while (bailout > 0
                && (firstIndividual == secondIndividual || firstIndividual.getScore().equals(secondIndividual.getScore()))) {
            secondIndividual = population.selectIndividual();
            bailout--;
        }
        Objects.requireNonNull(worker).applyCrossover(stepScope, firstIndividual, secondIndividual,
                individual -> population.addIndividual(individual, true));
        // Each individual produced contributes one additional movement. 
        // Therefore, the movement speed will be the number of produced individuals divided by time.
        phaseScope.getSolverScope().addMoveEvaluationCount(1L);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        var workerScoreDirector =
                phaseScope.<Score_> getScoreDirector().createChildThreadScoreDirector(ChildThreadType.MOVE_THREAD);
        var workerSolutionUpdater =
                new DefaultBestSolutionUpdater<>(phaseScope, configuration.bestSolutionRecaller(), phaseScope.getPopulation(),
                        configuration.solutionStateManager());
        this.worker =
                new HybridGeneticSearchWorker<>(configuration.constructionIndividualStrategy(),
                        configuration.localSearchPhase(),
                        configuration.refinementPhase(), configuration.crossoverStrategy(), configuration.individualBuilder(),
                        configuration.solutionStateManager(), workerSolutionUpdater, workerScoreDirector);
        this.worker.phaseStarted(phaseScope);
        this.lastBestIter = 0;
    }

    @Override
    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        Objects.requireNonNull(worker).phaseEnded(phaseScope);
        worker = null;
    }

    @Override
    public void stepStarted(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        if (lastBestIter == 0) {
            this.lastBestIter = phaseScope.getPopulation().getStatistics().individualCount();
        } else {
            var size = configuration.populationSize() - configuration.eliteSolutionSize();
            var restart = (phaseScope.getPopulation().getStatistics().individualCount() - lastBestIter) > configuration
                    .populationRestartCount();
            if (restart) {
                // Each individual produced contributes one additional movement. 
                // Therefore, the movement speed will be the number of produced individuals divided by time.
                Objects.requireNonNull(worker).restartPopulation(phaseScope, size,
                        individual -> stepScope.getPhaseScope().getSolverScope().addMoveEvaluationCount(1L));
                this.lastBestIter = phaseScope.getPopulation().getStatistics().individualCount();
            }
        }
    }

    @Override
    public void stepEnded(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        // Do nothing
    }

    @NullMarked
    @SuppressWarnings("rawtypes")
    public static class Builder<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>, Type_ extends EvolutionaryDecider> {

        int populationSize;
        int generationSize;
        int eliteSolutionSize;
        int populationRestartCount;
        @Nullable
        ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy;
        @Nullable
        Phase<Solution_> localSearchPhase;
        @Nullable
        Phase<Solution_> refinementPhase;
        @Nullable
        CrossoverStrategy<Solution_, Score_> crossoverStrategy;
        @Nullable
        IndividualBuilder<Solution_, Score_> individualBuilder;
        @Nullable
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager;
        @Nullable
        PhaseTermination<Solution_> phaseTermination;
        @Nullable
        BestSolutionRecaller<Solution_> bestSolutionRecaller;

        public Builder<Solution_, Score_, State_, Type_> withPopulationSize(int populationSize) {
            this.populationSize = populationSize;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withGenerationSize(int generationSize) {
            this.generationSize = generationSize;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withEliteSolutionSize(int eliteSolutionSize) {
            this.eliteSolutionSize = eliteSolutionSize;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withPopulationRestartCount(int populationRestartCount) {
            this.populationRestartCount = populationRestartCount;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_>
                withConstructionIndividualStrategy(
                        ConstructionIndividualStrategy<Solution_, Score_> constructionIndividualStrategy) {
            this.constructionIndividualStrategy = constructionIndividualStrategy;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withLocalSearchPhase(Phase<Solution_> localSearchPhase) {
            this.localSearchPhase = localSearchPhase;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withRefinementPhase(@Nullable Phase<Solution_> swapStarPhase) {
            this.refinementPhase = swapStarPhase;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_>
                withCrossoverStrategy(CrossoverStrategy<Solution_, Score_> crossoverStrategy) {
            this.crossoverStrategy = crossoverStrategy;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_>
                withIndividualBuilder(IndividualBuilder<Solution_, Score_> individualBuilder) {
            this.individualBuilder = individualBuilder;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_>
                withSolutionStateManager(SolutionStateManager<Solution_, Score_, State_> solutionInitializer) {
            this.solutionStateManager = solutionInitializer;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_> withPhaseTermination(PhaseTermination<Solution_> phaseTermination) {
            this.phaseTermination = phaseTermination;
            return this;
        }

        public Builder<Solution_, Score_, State_, Type_>
                withBestSolutionRecaller(BestSolutionRecaller<Solution_> bestSolutionRecaller) {
            this.bestSolutionRecaller = bestSolutionRecaller;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Type_ build() {
            return (Type_) new HybridGeneticSearchDecider<>(this);
        }
    }
}
