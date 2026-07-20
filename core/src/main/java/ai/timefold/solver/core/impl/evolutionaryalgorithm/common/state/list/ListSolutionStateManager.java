package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.list;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionStateManager;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.population.individual.Individual;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

import org.jspecify.annotations.NullMarked;

/**
 * Handles the saving and restoring of the working solution state for solutions using a {@link ListVariableDescriptor list
 * variable}.
 * <p>
 * {@link SolutionStateManager#saveSolutionState} captures a snapshot of the current working solution by cloning it
 * and recording which planning values are assigned to each planning entity.
 * <p>
 * {@link #restoreSolutionState} rolls the working solution back to a previously saved snapshot.
 * It selectively unassigns all values that are currently assigned and reassign the ones from the saved state.
 * <p>
 * This class is used by the evolutionary algorithm to reset the working solution to a clean baseline before generating
 * new individuals.
 */
@NullMarked
public final class ListSolutionStateManager<Solution_, Score_ extends Score<Score_>>
        implements SolutionStateManager<Solution_, Score_, ListSolutionState<Solution_, Score_>> {

    @Override
    public ListSolutionState<Solution_, Score_> saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            boolean saveAssigned) {
        var listVariableDescriptor = Objects.requireNonNull(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
        try (var listVariableSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor)) {
            var solution = scoreDirector.getWorkingSolution();
            var size =
                    (int) scoreDirector.getValueRangeManager().countOnSolution(listVariableDescriptor.getValueRangeDescriptor(),
                            solution) - listVariableSupply.getUnassignedCount();
            if (size == 0) {
                return new ListSolutionState<>(scoreDirector.cloneSolution(scoreDirector.getWorkingSolution()),
                        Collections.emptyList(), scoreDirector.calculateScore());
            }
            var valueRange =
                    scoreDirector.getValueRangeManager().getFromSolution(listVariableDescriptor.getValueRangeDescriptor(),
                            solution);
            var assignedValueList = new ArrayList<ListValueState>(size);
            for (var iterator = valueRange.createOriginalIterator(); iterator.hasNext();) {
                var value = iterator.next();
                if (saveAssigned && listVariableSupply.isAssigned(value)) {
                    assignedValueList
                            .add(new ListValueState(value, listVariableSupply.getElementPosition(value).ensureAssigned()));
                }
            }
            assignedValueList.sort(Comparator.comparing(ListValueState::index));
            return new ListSolutionState<>(scoreDirector.getWorkingSolution(), assignedValueList,
                    scoreDirector.calculateScore());
        }
    }

    @Override
    public ListSolutionState<Solution_, Score_> saveSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            Individual<Solution_, Score_> individual) {
        var assignedValues = Arrays.stream(individual.getChromosome())
                .map(chromosomeEntry -> new ListValueState(chromosomeEntry.value(),
                        ElementPosition.of(chromosomeEntry.entity(), chromosomeEntry.index())))
                .toList();
        return new ListSolutionState<>(individual.getSolution(), assignedValues, individual.getScore());
    }

    @Override
    public void restoreSolutionState(InnerScoreDirector<Solution_, Score_> scoreDirector,
            ListSolutionState<Solution_, Score_> stateToRestore) {
        var listVariableDescriptor = Objects.requireNonNull(scoreDirector.getSolutionDescriptor().getListVariableDescriptor());
        var listVariableMetaModel = listVariableDescriptor.getVariableMetaModel();
        try (var listVariableSupply = scoreDirector.getListVariableStateSupply(listVariableDescriptor)) {
            var solution = scoreDirector.getWorkingSolution();
            var size =
                    (int) scoreDirector.getValueRangeManager().countOnSolution(listVariableDescriptor.getValueRangeDescriptor(),
                            solution) - listVariableSupply.getUnassignedCount();
            var needRebase = stateToRestore.getSolution() != solution;
            var moveList = unassignAll(listVariableMetaModel, listVariableDescriptor, listVariableSupply, solution, size);
            for (var stateEntry : stateToRestore.stateList()) {
                if (needRebase) {
                    var rebasedValue = Objects.requireNonNull(scoreDirector.lookUpWorkingObject(stateEntry.value()));
                    var rebasedEntity =
                            Objects.requireNonNull(scoreDirector.lookUpWorkingObject(stateEntry.positionInList().entity()));
                    moveList.add(Moves.assign(listVariableMetaModel, rebasedValue, rebasedEntity,
                            stateEntry.positionInList().index()));
                } else {
                    moveList.add(Moves.assign(listVariableMetaModel, stateEntry.value(), stateEntry.positionInList()));
                }
            }
            if (!moveList.isEmpty()) {
                var compositeMove = Moves.compose(moveList);
                scoreDirector.getMoveDirector().execute(compositeMove);
            }
        }
    }

    private List<Move<Solution_>> unassignAll(
            PlanningListVariableMetaModel<Solution_, Object, Object> planningListVariableMetaModel,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableStateSupply<Solution_, Object, Object> listVariableSupply, Solution_ solution, int size) {
        var unassignMoveList = new ArrayList<Move<Solution_>>(size);
        var allEntities = listVariableDescriptor.getEntityDescriptor().extractEntities(solution);
        for (var entity : allEntities) {
            var start = listVariableDescriptor.getFirstUnpinnedIndex(entity);
            var end = listVariableDescriptor.getListSize(entity);
            var values = listVariableDescriptor.getValue(entity);
            for (var i = start; i < end; i++) {
                var value = values.get(i);
                if (!listVariableSupply.isPinned(value) && listVariableSupply.isAssigned(value)) {
                    unassignMoveList.add(Moves.unassign(planningListVariableMetaModel, entity, i));
                }
            }
        }
        Collections.reverse(unassignMoveList);
        return unassignMoveList;
    }
}
