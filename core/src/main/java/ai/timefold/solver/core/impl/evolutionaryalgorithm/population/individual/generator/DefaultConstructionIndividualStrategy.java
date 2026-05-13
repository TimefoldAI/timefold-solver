package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.custom.DefaultPhaseCommandContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates individuals for the initial population using construction heuristics followed by local search.
 * <p>
 * The first individual is built with a deterministic best-fit phase (always produces the same solution).
 * Every subsequent individual uses a shuffled best-fit phase to introduce diversity.
 * An optional list of custom {@link PhaseCommand}s may be applied beforehand to pre-shape the working solution.
 * <p>
 * The strategy can be used for both variable types, considering that the inner phases can handle each variable type.
 */
@NullMarked
public record DefaultConstructionIndividualStrategy<Solution_, Score_ extends Score<Score_>>(
        List<PhaseCommand<Solution_>> customPhaseIndividualCommandList, Phase<Solution_> deterministicBestFitConstructionPhase,
        Phase<Solution_> shuffledFirstFitConstructionPhase, Phase<Solution_> localSearchPhase,
        @Nullable Phase<Solution_> refinementPhase,
        IndividualBuilder<Solution_, Score_> individualBuilder) implements ConstructionIndividualStrategy<Solution_, Score_> {

    public DefaultConstructionIndividualStrategy(List<PhaseCommand<Solution_>> customPhaseIndividualCommandList,
            Phase<Solution_> deterministicBestFitConstructionPhase, Phase<Solution_> shuffledFirstFitConstructionPhase,
            Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            IndividualBuilder<Solution_, Score_> individualBuilder) {
        this.customPhaseIndividualCommandList = Objects.requireNonNull(customPhaseIndividualCommandList);
        this.deterministicBestFitConstructionPhase = Objects.requireNonNull(deterministicBestFitConstructionPhase);
        this.shuffledFirstFitConstructionPhase = Objects.requireNonNull(shuffledFirstFitConstructionPhase);
        this.localSearchPhase = Objects.requireNonNull(localSearchPhase);
        this.refinementPhase = refinementPhase;
        this.individualBuilder = Objects.requireNonNull(individualBuilder);
    }

    @Override
    public Individual<Solution_, Score_> apply(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = solverScope.<Score_> getScoreDirector();
        // The first step is to apply the custom phase commands, if any.
        // This allows to modify the working solution before the construction phase.
        if (!customPhaseIndividualCommandList.isEmpty()) {
            var commandContext = new DefaultPhaseCommandContext<>(stepScope.getMoveDirector(),
                    () -> phaseScope.getTermination().isPhaseTerminated(phaseScope));
            customPhaseIndividualCommandList.forEach(command -> command.changeWorkingSolution(commandContext));
        }
        updateScope(phaseScope);
        // Build and refine the solution
        applyPhases(phaseScope, getConstructionPhase(stepScope), localSearchPhase, refinementPhase);
        return individualBuilder.build(scoreDirector.cloneSolution(solverScope.getBestSolution()), solverScope.getBestScore(),
                null, null, scoreDirector);
    }

    private Phase<Solution_> getConstructionPhase(EvolutionaryAlgorithmStepScope<Solution_> stepScope) {
        if (stepScope.getPhaseScope().getPopulation().getBestIndividual() == null) {
            // The deterministic phase is used only once as its behavior always returns the same solution.
            // The shuffled phase is expected to shuffle the selector and produce different solutions.
            return deterministicBestFitConstructionPhase;
        }
        return shuffledFirstFitConstructionPhase;
    }
}
