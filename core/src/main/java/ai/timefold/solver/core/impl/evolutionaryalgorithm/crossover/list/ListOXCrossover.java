package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.fixIndex;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.generateIndexes;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverResult;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the OX crossover strategy for list variables.
 * The method incorporates genetic material from both parents into offspring by analyzing the solution as a single sequence of
 * planning values.
 * Let's consider a solution with two entities e1[v1, v2, v3] and e2[v4, v5].
 * The encoded solution is represented by a single sequence of planning values [v1, v2, v3, v4, v5].
 * <p>
 * Let's assume the cut point is [1, 3].
 * The planning values from the first parent to incorporate into the offspring are [v2, v3, v4].
 * The remaining values are added based on the solution provided by the second parent.
 */
@NullMarked
public final class ListOXCrossover<Solution_, Score_ extends Score<Score_>>
        extends AbstractListCrossover<Solution_, Score_> {

    private final boolean applyBestFitFirstPhase;

    public ListOXCrossover(Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            double inheritanceRate, boolean applyBestFitFirstPhase) {
        super(localSearchPhase, refinementPhase, inheritanceRate);
        this.applyBestFitFirstPhase = applyBestFitFirstPhase;
    }

    @Override
    public CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context) {
        var phaseScope = context.phaseScope();
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = phaseScope.<Score_> getScoreDirector();
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var listVariableModel = listVariableDescriptor.getVariableMetaModel();
        try (var listVariableStateSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor)) {
            var valueRangeManager = scoreDirector.getValueRangeManager();
            // Produce the offspring based on the two parents.
            generateOffspring(scoreDirector, listVariableStateSupply, listVariableDescriptor, listVariableModel,
                    valueRangeManager, context.firstIndividual(), context.secondIndividual(), inheritanceRate,
                    applyBestFitFirstPhase, phaseScope.getWorkingRandom());
            // We need to update the best solution, best score,
            // and initialized score to avoid inconsistencies in the next phases
            updateScope(phaseScope);
            applyPhases(phaseScope, localSearchPhase, refinementPhase);
            return new CrossoverResult<>(scoreDirector.cloneSolution(solverScope.getBestSolution()), solverScope.getBestScore(),
                    context.firstIndividual().getScore(), context.secondIndividual().getScore());
        }
    }

    private static <Solution_, Score_ extends Score<Score_>> void generateOffspring(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ValueRangeManager<Solution_> valueRangeManager, Individual<Solution_, Score_> firstIndividual,
            Individual<Solution_, Score_> secondIndividual, double inheritanceRate, boolean applyBestFitFirstPhase,
            RandomGenerator workingRandom) {
        var indexes = generateIndexes(workingRandom, firstIndividual.size(), inheritanceRate, true);
        var start = fixIndex(firstIndividual.getChromosome(), indexes[0], true);
        var end = fixIndex(firstIndividual.getChromosome(), indexes[1], false);
        // Add the values from the first parent within the specified interval
        var assignedValues = applyFirstPhase(scoreDirector, listVariableStateSupply, listVariableDescriptor,
                listVariableMetaModel, firstIndividual.getChromosome(), start, end, applyBestFitFirstPhase);
        // Add the remaining values from the second parent using the best-fit method with the provided sequence
        applyBestFit(scoreDirector, listVariableStateSupply, listVariableDescriptor, listVariableMetaModel, valueRangeManager,
                secondIndividual.getChromosome(), assignedValues);
    }

    /**
     * The values from the first parent are included in the same order they were provided.
     */
    private static <Solution_, Score_ extends Score<Score_>> Set<Object> applyFirstPhase(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ChromosomeEntry[] parentChromosome, int start, int end, boolean applyBestFit) {
        var assignedValues = CollectionUtils.newIdentityHashSet(end - start);
        for (var i = start; i < end; i++) {
            var entry = parentChromosome[i];
            var rebasedValue = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(entry.value()));
            if (applyBestFit) {
                applyBestFit(scoreDirector, listVariableStateSupply, listVariableMetaModel, listVariableDescriptor,
                        scoreDirector.getValueRangeManager(), rebasedValue, Collections.emptySet());
            } else {
                if (listVariableStateSupply.isPinned(rebasedValue) || listVariableStateSupply.isAssigned(rebasedValue)) {
                    continue;
                }
                var rebasedEntity = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(entry.entity()));
                scoreDirector.executeMove(Moves.assign(listVariableMetaModel, rebasedValue, rebasedEntity,
                        listVariableDescriptor.getListSize(rebasedEntity)));
            }
            assignedValues.add(rebasedValue);
        }
        return assignedValues;
    }
}
