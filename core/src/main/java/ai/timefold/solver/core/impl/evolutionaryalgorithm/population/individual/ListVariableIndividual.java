package ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Default representation of an individual for list variables.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
@NullMarked
public final class ListVariableIndividual<Solution_, Score_ extends Score<Score_>>
        extends AbstractIndividual<Solution_, Score_> {

    private final MemberAccessor planningIdAccessor;
    private final Map<Object, PositionPair> predecessorAndSuccessorMap;
    private final ChromosomeEntry[] chromosome;

    public ListVariableIndividual(InnerScoreDirector<Solution_, Score_> scoreDirector, Solution_ solution,
            InnerScore<Score_> score, @Nullable InnerScore<Score_> firstParentScore,
            @Nullable InnerScore<Score_> secondParentScore) {
        super(solution, score, firstParentScore, secondParentScore);
        var listVariableDescriptor = Objects.requireNonNull(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
        this.planningIdAccessor =
                scoreDirector.getSolutionDescriptor().getPlanningIdAccessor(listVariableDescriptor.getElementType());
        if (planningIdAccessor == null) {
            throw new IllegalStateException(
                    "The planning value class (%s) must include a planning value id field."
                            .formatted(listVariableDescriptor.getElementType()));
        }
        var size = (int) scoreDirector.getValueRangeManager().getProblemSizeStatistics().approximateValueCount();
        this.predecessorAndSuccessorMap = HashMap.newHashMap(size);
        var chromosomeList = new ArrayList<ChromosomeEntry>(size);
        load(listVariableDescriptor, planningIdAccessor, solution, chromosomeList, predecessorAndSuccessorMap);
        this.chromosome = chromosomeList.toArray(ChromosomeEntry[]::new);
    }

    private ListVariableIndividual(Solution_ solution, InnerScore<Score_> score, InnerScore<Score_> firstParentScore,
            InnerScore<Score_> secondParentScore, MemberAccessor planningIdAccessor,
            Map<Object, PositionPair> predecessorAndSuccessorMap, ChromosomeEntry[] chromosome) {
        super(solution, score, firstParentScore, secondParentScore);
        this.planningIdAccessor = planningIdAccessor;
        this.predecessorAndSuccessorMap = predecessorAndSuccessorMap;
        this.chromosome = chromosome;
    }

    private static <Solution_> void load(ListVariableDescriptor<Solution_> listVariableDescriptor,
            MemberAccessor planningIdAccessor, Solution_ solution, List<ChromosomeEntry> chromosomeList,
            Map<Object, PositionPair> predecessorAndSuccessorMap) {
        var allEntities = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        for (var entity : allEntities) {
            var valueList = listVariableDescriptor.getValue(entity);
            var size = valueList.size();
            if (size == 0) {
                continue;
            }
            // Collect all IDs in a single pass to avoid redundant accessor calls.
            var ids = new Object[size];
            for (var i = 0; i < size; i++) {
                var value = valueList.get(i);
                ids[i] = planningIdAccessor.executeGetter(value);
                chromosomeList.add(new ChromosomeEntry(entity, value, i));
            }
            for (var i = 0; i < size; i++) {
                predecessorAndSuccessorMap.put(ids[i],
                        new PositionPair(i > 0 ? ids[i - 1] : null, i < size - 1 ? ids[i + 1] : null));
            }
        }
    }

    @Override
    public ChromosomeEntry[] getChromosome() {
        return chromosome;
    }

    @Override
    public int size() {
        return predecessorAndSuccessorMap.size();
    }

    @Override
    public double diff(Individual<Solution_, Score_> otherIndividual) {
        var otherListIndividual = (ListVariableIndividual<Solution_, Score_>) otherIndividual;
        var diff = 0;
        for (var valueEntry : predecessorAndSuccessorMap.entrySet()) {
            var valuePosition = valueEntry.getValue();
            var otherValuePosition = otherListIndividual.predecessorAndSuccessorMap.get(valueEntry.getKey());
            if (otherValuePosition == null) {
                diff++;
                continue;
            }
            // No match like: [0, 1] and [1, 0]
            if (!Objects.equals(valuePosition.successor(), otherValuePosition.successor())
                    && !Objects.equals(valuePosition.successor(), otherValuePosition.predecessor())) {
                diff++;
            }
            // No match between the first element and the last element of each value
            if (valuePosition.predecessor() == null && otherValuePosition.predecessor() != null
                    && otherValuePosition.successor() != null) {
                diff++;
            }
        }
        return (double) diff / (double) predecessorAndSuccessorMap.size();
    }

    @Override
    public Individual<Solution_, Score_> clone(InnerScoreDirector<Solution_, Score_> scoreDirector) {
        var newSolution = scoreDirector.cloneSolution(solution);
        var newPredecessorAndSuccessorMap = HashMap.<Object, PositionPair> newHashMap(predecessorAndSuccessorMap.size());
        var chromosomeList = new ArrayList<ChromosomeEntry>(chromosome.length);
        load(scoreDirector.getSolutionDescriptor().getListVariableDescriptor(), planningIdAccessor, solution, chromosomeList,
                newPredecessorAndSuccessorMap);
        var newChromosome = chromosomeList.toArray(ChromosomeEntry[]::new);
        return new ListVariableIndividual<>(newSolution, score, firstParentScore, secondParentScore,
                planningIdAccessor, newPredecessorAndSuccessorMap, newChromosome);
    }

    private record PositionPair(@Nullable Object predecessor, @Nullable Object successor) {

    }
}
