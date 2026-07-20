package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;

import org.jspecify.annotations.NullMarked;

/**
 * Single-threaded implementation of {@link BestSolutionUpdater}.
 * <p>
 * When the step individual is the population's current best, this updater transfers that individual's
 * state to the shared phase scope's score director and notifies the {@link BestSolutionRecaller}.
 * <p>
 * Rather than calling {@code scoreDirector.setWorkingSolution}, which would trigger a full re-read of
 * all planning and fact values, the transfer is performed through the {@link SolutionStateManager}:
 * the individual's variable values are saved once from the worker's score director and then applied
 * directly to the shared score director, requiring only one additional read pass.
 */
@NullMarked
public record DefaultBestSolutionUpdater<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope,
        BestSolutionRecaller<Solution_> sharedBestSolutionRecaller, Population<Solution_, Score_> sharedPopulation,
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager)
        implements
            BestSolutionUpdater<Solution_> {

    @Override
    public void updateBestSolution(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var newIndividual = stepScope.<Score_> getStepIndividual();
        if (newIndividual != null && sharedPopulation.getBestIndividual() == stepScope.getStepIndividual()) {
            // The proposed approach avoids using `scoreDirector::setWorkingSolution`
            // to prevent the need to read all planning and fact values and recalculate statistics.
            // Instead,
            // it uses the solution state manager to save the individual state.
            // This method reads the values once and then assigns them to the current working solution,
            // requiring one additional read of the values.
            var individualState =
                    solutionStateManager.saveSolutionState(stepScope.getScoreDirector(), newIndividual);
            solutionStateManager.restoreSolutionState(sharedPhaseScope.getScoreDirector(), individualState);
            var bestSolutionStepScope = new EvolutionaryAlgorithmStepScope<>(sharedPhaseScope, newIndividual);
            bestSolutionStepScope.setScore(newIndividual.getScore());
            // The shared scope has the flag for triggering the best solution events set to true
            sharedBestSolutionRecaller.processWorkingSolutionDuringStep(bestSolutionStepScope);
        }
        // The method is always triggered after an individual is added to the population,
        // and it must be counted as a completed step
        sharedPhaseScope.setLastCompletedStepScope(stepScope);
    }
}
