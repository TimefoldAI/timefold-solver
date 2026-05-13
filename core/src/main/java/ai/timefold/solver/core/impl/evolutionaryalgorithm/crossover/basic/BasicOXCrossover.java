package ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.basic;

import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.fixIndex;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils.generateIndexes;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.applyPhases;
import static ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorker.updateScope;

import java.util.Objects;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.Utils;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverContext;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverResult;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.crossover.CrossoverStrategy;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.ChromosomeEntry;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of the OX crossover strategy for basic planning variables.
 * <p>
 * The solution is viewed as a flat array of (entity, variable-index) slots, ordered by entity iteration then variable index
 * within {@link EntityDescriptor#getBasicVariableDescriptorList()}.
 * A [start, end) cut-point interval is selected from the first parent's chromosome; slots in that interval inherit
 * the first parent's values, and the remaining slots inherit the second parent's values.
 * <p>
 * {@link Utils#fixIndex} ensures the cut never splits the basic variables of a single entity across parents.
 */
@NullMarked
public record BasicOXCrossover<Solution_, Score_ extends Score<Score_>>(Phase<Solution_> localSearchPhase,
        @Nullable Phase<Solution_> refinementPhase, double inheritanceRate) implements CrossoverStrategy<Solution_, Score_> {

    @Override
    public CrossoverResult<Solution_, Score_> apply(CrossoverContext<Solution_, Score_> context) {
        var phaseScope = context.phaseScope();
        var solverScope = phaseScope.getSolverScope();
        var scoreDirector = phaseScope.<Score_> getScoreDirector();
        generateOffspring(scoreDirector, context.firstIndividual(), context.secondIndividual(),
                inheritanceRate, phaseScope.getWorkingRandom());
        updateScope(phaseScope);
        applyPhases(phaseScope, localSearchPhase, refinementPhase);
        return new CrossoverResult<>(scoreDirector.cloneSolution(solverScope.getBestSolution()),
                solverScope.getBestScore(),
                context.firstIndividual().getScore(), context.secondIndividual().getScore());
    }

    private static <Solution_, Score_ extends Score<Score_>> void generateOffspring(
            InnerScoreDirector<Solution_, Score_> scoreDirector, Individual<Solution_, Score_> firstIndividual,
            Individual<Solution_, Score_> secondIndividual, double inheritanceRate, RandomGenerator workingRandom) {
        var p1 = firstIndividual.getChromosome();
        var p2 = secondIndividual.getChromosome();
        var indexes = generateIndexes(workingRandom, p1.length, inheritanceRate, true);
        var start = fixIndex(p1, indexes[0], true);
        var end = fixIndex(p1, indexes[1], false);
        var solutionDescriptor = scoreDirector.getSolutionDescriptor();
        inheritChromosome(scoreDirector, solutionDescriptor, p1, start, end);
        inheritChromosome(scoreDirector, solutionDescriptor, p2, 0, start);
        inheritChromosome(scoreDirector, solutionDescriptor, p2, end, p2.length);
    }

    private static <Solution_, Score_ extends Score<Score_>> void inheritChromosome(
            InnerScoreDirector<Solution_, Score_> scoreDirector, SolutionDescriptor<Solution_> solutionDescriptor,
            ChromosomeEntry[] chromosome, int from, int to) {
        EntityDescriptor<Solution_> entityDescriptor = null;
        for (var i = from; i < to; i++) {
            var entry = chromosome[i];
            if (entityDescriptor == null || entityDescriptor.getEntityClass() != entry.entity().getClass()) {
                entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(entry.entity().getClass());
            }
            var varDescriptor = entityDescriptor.getBasicVariableDescriptorList().get(entry.index());
            var rebasedEntity = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(entry.entity()));
            var rebasedValue = entry.value() != null ? scoreDirector.lookUpWorkingObject(entry.value()) : null;
            scoreDirector.executeMove(Moves.change(varDescriptor.getVariableMetaModel(), rebasedEntity, rebasedValue));
        }
    }
}
