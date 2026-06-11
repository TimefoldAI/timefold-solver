package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.list;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.fixIndex;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list.AbstractListCrossover.applyBestFit;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.scope.EvolutionaryAlgorithmStepScope;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.IndividualBuilder;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.ConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.generator.DefaultConstructionIndividualStrategy;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.custom.DefaultPhaseCommandContext;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates individuals for the population by applying a ruin-and-recreate method to the current best individual,
 * followed by local search.
 * <p>
 * When the population is empty the first individual is built using a deterministic best-fit construction phase,
 * identical to {@link DefaultConstructionIndividualStrategy}.
 * For every subsequent individual the strategy selects a random contiguous segment from the best individual,
 * unassigns those values (ruin phase), and reinserts them via best-fit insertion
 * (recreate phase) before running the local search. The segment boundaries are snapped to entity borders so that
 * all values belonging to the same entity are always ruined or kept together.
 */
@NullMarked
public record ListRuinRecreateIndividualStrategy<Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>(
        List<PhaseCommand<Solution_>> customPhaseIndividualCommandList, Phase<Solution_> deterministicBestFitConstructionPhase,
        Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
        SolutionStateManager<Solution_, Score_, State_> solutionStateManager,
        IndividualBuilder<Solution_, Score_> individualBuilder,
        double inheritanceRate) implements ConstructionIndividualStrategy<Solution_, Score_> {

    public ListRuinRecreateIndividualStrategy(List<PhaseCommand<Solution_>> customPhaseIndividualCommandList,
            Phase<Solution_> deterministicBestFitConstructionPhase, Phase<Solution_> localSearchPhase,
            @Nullable Phase<Solution_> refinementPhase, SolutionStateManager<Solution_, Score_, State_> solutionStateManager,
            IndividualBuilder<Solution_, Score_> individualBuilder, double inheritanceRate) {
        this.customPhaseIndividualCommandList = Objects.requireNonNull(customPhaseIndividualCommandList);
        this.deterministicBestFitConstructionPhase = Objects.requireNonNull(deterministicBestFitConstructionPhase);
        this.localSearchPhase = Objects.requireNonNull(localSearchPhase);
        this.refinementPhase = refinementPhase;
        this.solutionStateManager = solutionStateManager;
        this.individualBuilder = Objects.requireNonNull(individualBuilder);
        this.inheritanceRate = inheritanceRate;
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
        updateScope(stepScope.getPhaseScope());
        // If the population has no best individual, use the deterministic construction phase
        var population = phaseScope.<Score_> getPopulation();
        if (stepScope.getBestIndividual() == null) {
            applyPhases(phaseScope, deterministicBestFitConstructionPhase, localSearchPhase, refinementPhase);
        } else {
            applyRuinRecreate(solverScope, scoreDirector, Objects.requireNonNull(stepScope.getBestIndividual()));
            updateScope(stepScope.getPhaseScope());
            applyPhases(phaseScope, localSearchPhase, refinementPhase);
        }
        return individualBuilder.build(scoreDirector.cloneSolution(solverScope.getBestSolution()), solverScope.getBestScore(),
                null, null, scoreDirector);
    }

    @Override
    public Phase<Solution_> getLocalSearchPhase() {
        return localSearchPhase;
    }

    @Override
    public @Nullable Phase<Solution_> getRefinementPhase() {
        return refinementPhase;
    }

    void applyRuinRecreate(SolverScope<Solution_> solverScope, InnerScoreDirector<Solution_, Score_> scoreDirector,
            Individual<Solution_, Score_> bestIndividual) {
        var bestSolutionState = solutionStateManager.saveSolutionState(scoreDirector, bestIndividual);
        solutionStateManager.restoreSolutionState(scoreDirector, bestSolutionState);
        var listVariableDescriptor = Objects.requireNonNull(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
        var listVariableMetaModel = listVariableDescriptor.getVariableMetaModel();
        var valueRangeManager = scoreDirector.getValueRangeManager();
        try (var listVariableStateSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor)) {
            var ruinedValues = applyRuinPhase(scoreDirector, listVariableStateSupply, listVariableMetaModel,
                    solverScope.getWorkingRandom(), bestIndividual);
            Collections.shuffle(ruinedValues, solverScope.getWorkingRandom());
            applyRecreatePhase(scoreDirector, listVariableStateSupply, listVariableMetaModel, listVariableDescriptor,
                    valueRangeManager, ruinedValues);
        }
    }

    private List<Object> applyRuinPhase(InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel, RandomGenerator workingRandom,
            Individual<Solution_, Score_> bestIndividual) {
        var indexes = generateIndexes(workingRandom, bestIndividual.size(), inheritanceRate);
        var start = fixIndex(bestIndividual.getChromosome(), indexes[0], true);
        var end = fixIndex(bestIndividual.getChromosome(), indexes[1], false);
        var chromosome = bestIndividual.getChromosome();
        var moveList = new ArrayList<Move<Solution_>>(end - start);
        var ruinedValues = new ArrayList<>(end - start);
        for (var i = start; i < end; i++) {
            var rebasedValue = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(chromosome[i].value()));
            if (listVariableStateSupply.isPinned(rebasedValue)) {
                continue;
            }
            var position = listVariableStateSupply.getElementPosition(rebasedValue).ensureAssigned();
            moveList.add(Moves.unassign(listVariableMetaModel, position));
            ruinedValues.add(rebasedValue);
        }
        Collections.reverse(moveList);
        if (!moveList.isEmpty()) {
            scoreDirector.getMoveDirector().execute(Moves.compose(moveList));
        }
        return ruinedValues;
    }

    private void applyRecreatePhase(InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ListVariableDescriptor<Solution_> listVariableDescriptor, ValueRangeManager<Solution_> valueRangeManager,
            List<Object> ruinedValues) {
        for (var value : ruinedValues) {
            applyBestFit(scoreDirector, listVariableStateSupply, listVariableMetaModel, listVariableDescriptor,
                    valueRangeManager, value, Collections.emptySet());
        }
    }

    private static int[] generateIndexes(RandomGenerator workingRandom, int size, double inheritanceRate) {
        var start = workingRandom.nextInt(size);
        // An inheritance rate of 95% means no more than 5% of the solution can be ruined.
        // Some experiments have shown that a higher rate is more effective for overconstrained models
        var maxSize = size * (1 - inheritanceRate);
        var end = Math.min((int) (start + maxSize), size - 1);
        return new int[] { start, end };
    }
}
