package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.list;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.Collections;
import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverResult;
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
 * Implementation of the Route Exchange (RX) crossover strategy for list variables.
 * <p>
 * Unlike {@link ListOXCrossover}, which operates on a flat positional cut across all entities, RX treats each
 * entity's complete route as the unit of inheritance. For each entity in the working solution, a coin flip determines
 * whether its route is inherited from the first or the second parent. The values from the winning parent's route are
 * placed on that entity preserving the within-route order from the parent.
 * <p>
 * Values not placed during the entity inheritance phase — because they belonged to a route whose entity chose the other
 * parent, and all those values were already claimed — are placed in a second phase that follows P2's chromosome order
 * and selects the best-scoring entity for each remaining value, identical to the second-parent phase of OX.
 * <p>
 * This operator follows the same pattern as the Selective Route Exchange (SREX) used in the Hybrid Genetic Search (HGS)
 * literature for vehicle routing problems (Vidal et al.).
 */
@NullMarked
public final class ListRXCrossover<Solution_, Score_ extends Score<Score_>>
        extends AbstractListCrossover<Solution_, Score_> {

    private final boolean applyBestFitFirstPhase;

    public ListRXCrossover(Phase<Solution_> localSearchPhase, @Nullable Phase<Solution_> refinementPhase,
            double inheritanceRage, boolean applyBestFitFirstPhase, RandomGenerator randomGenerator) {
        super(localSearchPhase, refinementPhase, inheritanceRage, randomGenerator);
        this.applyBestFitFirstPhase = applyBestFitFirstPhase;
    }

    @Override
    public CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context) {
        var phaseScope = context.phaseScope();
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = phaseScope.<Score_> getScoreDirector();
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var listVariableMetaModel = listVariableDescriptor.getVariableMetaModel();
        try (var listVariableStateSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor)) {
            var valueRangeManager = scoreDirector.getValueRangeManager();
            // Produce the offspring based on the two parents.
            // The offspring is expected to inherit approximately 90% of their planning values from the first parent.
            // Some experiments have demonstrated that this approach is more effective in overconstrained models.
            generateOffspring(scoreDirector, listVariableStateSupply, listVariableDescriptor, listVariableMetaModel,
                    valueRangeManager, context.firstIndividual(), context.secondIndividual(), workingRandom, inheritanceRate,
                    applyBestFitFirstPhase);
            // We need to update the best solution, best score,
            // and initialized score to avoid inconsistencies in the next phases
            updateScope(phaseScope);
            applyPhases(phaseScope, localSearchPhase, refinementPhase);
            return new CrossoverResult<>(scoreDirector.cloneSolution(solverScope.getBestSolution()), solverScope.getBestScore(),
                    context.firstIndividual().getScore(), context.secondIndividual().getScore());
        }
    }

    @Override
    public Phase<Solution_> getLocalSearchPhase() {
        return localSearchPhase;
    }

    @Override
    public @Nullable Phase<Solution_> getRefinementPhase() {
        return refinementPhase;
    }

    private static <Solution_, Score_ extends Score<Score_>> void generateOffspring(
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListVariableStateSupply<Solution_, Object, Object> listVariableStateSupply,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            PlanningListVariableMetaModel<Solution_, Object, Object> listVariableMetaModel,
            ValueRangeManager<Solution_> valueRangeManager, Individual<Solution_, Score_> firstIndividual,
            Individual<Solution_, Score_> secondIndividual, RandomGenerator workingRandom, double firstParentInheritanceRate,
            boolean applyBestFitFirstPhase) {

        var workingSolution = scoreDirector.getWorkingSolution();
        var allEntities = listVariableDescriptor.getEntityDescriptor().extractEntities(workingSolution);
        var p1Entities = listVariableDescriptor.getEntityDescriptor().extractEntities(firstIndividual.getSolution());
        var p2Entities = listVariableDescriptor.getEntityDescriptor().extractEntities(secondIndividual.getSolution());
        var assignedValues = CollectionUtils.newIdentityHashSet(firstIndividual.size());

        // Phase 1: for each entity, inherit its complete route from a randomly chosen parent.
        // Values already claimed by an earlier entity (duplicates across parents) are skipped.
        for (var i = 0; i < allEntities.size(); i++) {
            var entity = allEntities.get(i);
            var parentEntity = workingRandom.nextDouble() < firstParentInheritanceRate ? p1Entities.get(i) : p2Entities.get(i);
            var parentValueList = listVariableDescriptor.getValue(parentEntity);
            for (var value : parentValueList) {
                var rebasedValue = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(value));
                if (applyBestFitFirstPhase) {
                    applyBestFit(scoreDirector, listVariableStateSupply, listVariableMetaModel, listVariableDescriptor,
                            scoreDirector.getValueRangeManager(), rebasedValue, Collections.emptySet());
                } else {
                    if (listVariableStateSupply.isPinned(rebasedValue) || listVariableStateSupply.isAssigned(rebasedValue)) {
                        continue;
                    }
                    scoreDirector.executeMove(
                            Moves.assign(listVariableMetaModel, rebasedValue, entity,
                                    listVariableDescriptor.getListSize(entity)));
                }
                assignedValues.add(rebasedValue);
            }
        }

        // Phase 2: assign remaining values in P2's chromosome order using cross-entity best-fit.
        applyBestFit(scoreDirector, listVariableStateSupply, listVariableDescriptor, listVariableMetaModel, valueRangeManager,
                secondIndividual.getChromosome(), assignedValues);
    }
}
