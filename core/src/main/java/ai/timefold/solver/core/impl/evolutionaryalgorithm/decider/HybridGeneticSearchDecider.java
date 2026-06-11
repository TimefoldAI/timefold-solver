package ai.timefold.solver.core.impl.evolutionaryalgorithm.decider;

import static ai.timefold.solver.core.impl.solver.thread.ChildThreadType.EVOLUTIONARY_AGENT_THREAD;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution.DefaultBestSolutionUpdater;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.DefaultPopulation;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.solver.random.DelegatingSplittableRandomGenerator;

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
        extends AbstractHybridGeneticSearchDecider<Solution_, Score_> {

    private final HybridGeneticSearchWorkerContext<Solution_, Score_, State_> context;

    @Nullable
    private HybridGeneticSearchWorker<Solution_, Score_, State_> worker = null;

    public HybridGeneticSearchDecider(AbstractBuilder<Solution_, Score_, State_> builder) {
        super(builder.logIndentation, builder.populationSize, builder.generationSize, builder.eliteSolutionSize,
                builder.populationRestartCount, Objects.requireNonNull(builder.bestSolutionRecaller));
        this.context = Objects.requireNonNull(builder.context);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public Population<Solution_, Score_> emptyPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        return new DefaultPopulation<>(populationSize, generationSize, eliteSolutionSize);
    }

    @Override
    public void loadPopulation(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        var population = phaseScope.<Score_> getPopulation();
        var nonNullWorker = Objects.requireNonNull(worker);
        while (population.size() < populationSize) {
            if (phaseScope.getTermination().isPhaseTerminated(phaseScope)) {
                break;
            }
            nonNullWorker.generateIndividual(phaseScope, population::getBestIndividual,
                    individual -> population.addIndividual(individual, false));
            // Each individual produced contributes one additional movement. 
            // Therefore, the movement speed will be the number of produced individuals divided by time.
            phaseScope.getSolverScope().addMoveEvaluationCount(1L);
        }
    }

    @Override
    public void evolvePopulation(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var population = phaseScope.<Score_> getPopulation();
        var firstIndividual = population.selectIndividual(stepScope.getWorkingRandom());
        var secondIndividual = population.selectIndividual(stepScope.getWorkingRandom());
        var bailout = population.size();
        while (bailout > 0
                && (firstIndividual == secondIndividual || firstIndividual.getScore().equals(secondIndividual.getScore()))) {
            secondIndividual = population.selectIndividual(stepScope.getWorkingRandom());
            bailout--;
        }
        Objects.requireNonNull(worker).applyCrossover(stepScope, firstIndividual, secondIndividual,
                individual -> population.addIndividual(individual, true));
        // Each individual produced contributes one additional movement. 
        // Therefore, the movement speed will be the number of produced individuals divided by time.
        phaseScope.getSolverScope().addMoveEvaluationCount(1L);
    }

    @Override
    public void restart(EvolutionaryAlgorithmStepScope<Solution_> stepScope, int size) {
        // Each individual produced contributes one additional movement. 
        // Therefore, the movement speed will be the number of produced individuals divided by time.
        var population = stepScope.<Score_> getPopulation();
        Objects.requireNonNull(worker).restartPopulation(stepScope.getPhaseScope(), size,
                population::getBestIndividual,
                individual -> stepScope.getPhaseScope().getSolverScope().addMoveEvaluationCount(1L));
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void phaseStarted(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        var workerSolverScope = phaseScope.getSolverScope().createChildThreadSolverScope(EVOLUTIONARY_AGENT_THREAD);
        var bestSolutionUpdater = new DefaultBestSolutionUpdater<>(phaseScope, bestSolutionRecaller, phaseScope.getPopulation(),
                context.solutionStateManager());
        // The proposed HGS implementation can use two optimization profiles:
        // one exploratory with a higher perturbation rate and another conservative approach with a lower perturbation rate.
        // Experiments with academic instances have shown some benefits
        // from using the conservative profile for problems
        // that begin with 50 planning entities and 200 planning values.
        // We use the scale metric to measure complexity, a value that is already computed by the solver.
        // While this metric is not flawless,
        // it helps prioritize the conservative profile when addressing complex problems.
        // Specifically,
        // this occurs
        // when the combination of entities and values results in a scale metric
        // that is greater than or equal to the one observed in the experiments: 427.
        var exploratoryRate = context.exploratoryRate();
        if (exploratoryRate == -1) { // Not set by the user
            var scaleLog = phaseScope.getSolverScope().getScoreDirector().getValueRangeManager().getProblemSizeStatistics()
                    .approximateProblemSizeLog();
            // It is expected that the conservative profile will be called 90% of the time for complex problems
            exploratoryRate = scaleLog < 427.0 ? 0.9 : 0.1;
        }
        this.worker = new HybridGeneticSearchWorker<>(HybridGeneticSearchWorkerContext.of(exploratoryRate, context),
                bestSolutionUpdater, (DelegatingSplittableRandomGenerator) workerSolverScope.getWorkingRandom(),
                workerSolverScope);
        this.worker.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(EvolutionaryAlgorithmPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        Objects.requireNonNull(worker).phaseEnded(phaseScope);
        worker = null;
    }

    @NullMarked
    public static class Builder<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            extends AbstractHybridGeneticSearchDecider.AbstractBuilder<Solution_, Score_, State_> {

        @SuppressWarnings("unchecked")
        public HybridGeneticSearchDecider<Solution_, Score_, State_> build() {
            return new HybridGeneticSearchDecider<>(this);
        }
    }
}
