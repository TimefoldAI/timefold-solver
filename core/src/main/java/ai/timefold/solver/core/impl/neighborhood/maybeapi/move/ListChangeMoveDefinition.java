package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.EnumeratingJoiners;
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

    public ListChangeMoveDefinition(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var entityValuePairs = moveStreamFactory.forEachAssignablePosition(variableMetaModel);
        var availableValues = moveStreamFactory.forEach(variableMetaModel.type(), false);
        return moveStreamFactory.pick(entityValuePairs)
                .pick(availableValues, EnumeratingJoiners.filtering(this::isValidChange))
                .asMove((solutionView, targetPosition, value) -> {
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

    private boolean isValidChange(SolutionView<Solution_> solutionView, ElementPosition targetPosition, Value_ value) {
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
