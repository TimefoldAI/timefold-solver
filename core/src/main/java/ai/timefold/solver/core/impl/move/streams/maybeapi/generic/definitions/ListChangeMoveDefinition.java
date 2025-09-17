package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.definitions;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.DataJoiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.Moves;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveDefinition;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * For each unassigned value, creates a move to assign it to some position of some list variable.
 * For each assigned value that is not pinned, creates:
 * 
 * <ul>
 * <li>A move to unassign it.</li>
 * <li>A move to reassign it to another position if assigned.</li>
 * </ul>
 * 
 * To assign or reassign a value, creates:
 * 
 * <ul>
 * <li>A move for every unpinned value in every entity's list variable to assign the value before that position.</li>
 * <li>A move for every entity to assign it to the last position in the list variable.</li>
 * </ul>
 *
 * This is a generic move provider that works with any list variable;
 * user-defined change move providers needn't be this complex, as they understand the specifics of the domain.
 */
@NullMarked
public class ListChangeMoveDefinition<Solution_, Entity_, Value_>
        implements MoveDefinition<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final BiDataFilter<Solution_, Entity_, Value_> isValueInListFilter;

    public ListChangeMoveDefinition(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.isValueInListFilter = (solution, entity, value) -> {
            if (entity == null || value == null) {
                // Necessary for the null to survive until the later stage,
                // where we will use it as a special marker to either unassign the value,
                // or move it to the end of list.
                return true;
            }
            return solution.isValueInRange(variableMetaModel, entity, value);
        };
    }

    @Override
    public MoveProducer<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        // Stream with unpinned entities;
        // includes null if the variable allows unassigned values.
        var unpinnedEntities =
                moveStreamFactory.forEach(variableMetaModel.entity().type(), variableMetaModel.allowsUnassignedValues());
        // Stream with unpinned values, which are assigned to any list variable;
        // always includes null so that we can later create a position at the end of the list,
        // i.e. with no value after it.
        var unpinnedValues = moveStreamFactory.forEach(variableMetaModel.type(), true)
                .filter((solutionView, value) -> value == null
                        || solutionView.getPositionOf(variableMetaModel, value) instanceof PositionInList);
        // Joins the two previous streams to create pairs of (entity, value),
        // eliminating values which do not match that entity's value range.
        // It maps these pairs to expected target positions in that entity's list variable.
        var entityValuePairs = unpinnedEntities.join(unpinnedValues, DataJoiners.filtering(isValueInListFilter))
                .map((solutionView, entity, value) -> {
                    if (entity == null) { // Null entity means we need to unassign the value.
                        return ElementPosition.unassigned();
                    }
                    var valueCount = solutionView.countValues(variableMetaModel, entity);
                    if (value == null || valueCount == 0) { // This will trigger assignment of the value at the end of the list.
                        return ElementPosition.of(entity, valueCount);
                    } else { // This will trigger assignment of the value immediately before this value.
                        return solutionView.getPositionOf(variableMetaModel, value);
                    }
                })
                .distinct();
        // Finally the stream of these positions is joined with the stream of all existing values,
        // filtering out those which would not result in a valid move.
        var dataStream = moveStreamFactory.forEach(variableMetaModel.type(), false)
                .join(entityValuePairs, DataJoiners.filtering(this::isValidChange));
        // When picking from this stream, we decide what kind of move we need to create,
        // based on whether the value is assigned or unassigned.
        return moveStreamFactory.pick(dataStream)
                .asMove((solutionView, value, targetPosition) -> {
                    var currentPosition = solutionView.getPositionOf(variableMetaModel, Objects.requireNonNull(value));
                    if (targetPosition instanceof UnassignedElement) {
                        var currentElementPosition = currentPosition.ensureAssigned();
                        return Moves.unassign(currentElementPosition, variableMetaModel);
                    }
                    var targetElementPosition = Objects.requireNonNull(targetPosition).ensureAssigned();
                    if (currentPosition instanceof UnassignedElement) {
                        return Moves.assign(value, targetElementPosition, variableMetaModel);
                    }
                    var currentElementPosition = currentPosition.ensureAssigned();
                    return Moves.change(currentElementPosition, targetElementPosition, variableMetaModel);
                });
    }

    private boolean isValidChange(SolutionView<Solution_> solutionView, Value_ value, ElementPosition targetPosition) {
        var currentPosition = solutionView.getPositionOf(variableMetaModel, value);
        if (currentPosition.equals(targetPosition)) { // No change needed.
            return false;
        }

        if (currentPosition instanceof UnassignedElement) { // Only assign the value if the target entity will accept it.
            var targetPositionInList = targetPosition.ensureAssigned();
            return solutionView.isValueInRange(variableMetaModel, targetPositionInList.entity(), value);
        }

        if (!(targetPosition instanceof PositionInList targetPositionInList)) { // Unassigning a value.
            return true;
        }

        var currentPositionInList = currentPosition.ensureAssigned();
        if (currentPositionInList.entity() == targetPositionInList.entity()) { // The value is already in the list.
            var valueCount = solutionView.countValues(variableMetaModel, currentPositionInList.entity());
            if (valueCount == 1) { // The value is the only value in the list; no change.
                return false;
            } else if (targetPositionInList.index() == valueCount) { // Trying to move the value past the end of the list.
                return false;
            } else { // Same list, same position; ignore.
                return currentPositionInList.index() != targetPositionInList.index();
            }
        }

        // We can move freely between entities, assuming the target entity accepts the value.
        return solutionView.isValueInRange(variableMetaModel, targetPositionInList.entity(), value);
    }

}
