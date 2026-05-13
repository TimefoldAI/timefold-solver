package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.bestsolution;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmPhaseScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.Population;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultBestSolutionUpdater<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
        implements BestSolutionUpdater<Solution_> {

    private final EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope;
    private final BestSolutionRecaller<Solution_> sharedBestSolutionRecaller;
    private final Population<Solution_, Score_> sharedPopulation;
    private final SolutionStateManager<Solution_, Score_, State_> solutionStateManager;

    public DefaultBestSolutionUpdater(EvolutionaryAlgorithmPhaseScope<Solution_> sharedPhaseScope,
            BestSolutionRecaller<Solution_> sharedBestSolutionRecaller, Population<Solution_, Score_> sharedPopulation,
            SolutionStateManager<Solution_, Score_, State_> solutionStateManager) {
        this.sharedPhaseScope = sharedPhaseScope;
        this.sharedBestSolutionRecaller = sharedBestSolutionRecaller;
        this.sharedPopulation = sharedPopulation;
        this.solutionStateManager = solutionStateManager;
    }

    @Override
    public void updateBestSolution(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var newIndividual = stepScope.getStepIndividual();
        if (sharedPopulation.getBestIndividual() == stepScope.getStepIndividual()) {
            // The proposed approach avoids using `scoreDirector::setWorkingSolution`
            // to prevent the need to read all planning and fact values and recalculate statistics.
            // Instead,
            // it uses the solution state manager to save the individual state.
            // This method reads the values once and then assigns them to the current working solution,
            // requiring one additional read of the values.
            var individualState =
                    solutionStateManager.saveSolutionState(stepScope.getScoreDirector(), stepScope.getStepIndividual());
            solutionStateManager.restoreSolutionState(sharedPhaseScope.getScoreDirector(), individualState);
            var bestSolutionStepScope = new EvolutionaryAlgorithmStepScope<>(sharedPhaseScope, newIndividual);
            bestSolutionStepScope.setScore(newIndividual.getScore());
            var oldState = sharedBestSolutionRecaller.isEnableUpdateEvents();
            sharedBestSolutionRecaller.setEnableUpdateEvents(true);
            sharedBestSolutionRecaller.processWorkingSolutionDuringStep(bestSolutionStepScope);
            sharedBestSolutionRecaller.setEnableUpdateEvents(oldState);
        }
    }
}
