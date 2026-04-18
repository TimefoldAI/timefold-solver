package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

/**
 * For each assigned value that is not pinned, creates a move to reassign it to a different position in a list variable.
 * Unassigned-to-list (assign) moves are handled by {@code ListAssignMoveProvider}.
 * List-to-unassigned (unassign) moves are handled by {@code ListUnassignMoveProvider}.
 *
 * <p>
 * To reassign a value, creates:
 *
 * <ul>
 * <li>A move for every unpinned position in every entity's list variable to reassign the value before that position.</li>
 * <li>A move for every entity to reassign the value to the last position in the list variable.</li>
 * </ul>
 *
 * This is a generic move provider that works with any list variable;
 * user-defined change move providers needn't be this complex, as they understand the specifics of the domain.
 */
@NullMarked
public class ListChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var entityValuePairs = moveStreamFactory.forEachDestination(variableMetaModel);
        var assignedValues = moveStreamFactory.forEachAssignedValue(variableMetaModel);
        return moveStreamFactory.pick(entityValuePairs)
                .pick(assignedValues,
                        NeighborhoodsJoiners.filtering(this::isValidChange))
                .asMove((solutionView, targetPosition, value) -> {
                    var currentPosition = solutionView.getPositionOf(variableMetaModel, Objects.requireNonNull(value))
                            .ensureAssigned();
                    return Moves.change(variableMetaModel, currentPosition, targetPosition);
                });
    }

    private boolean isValidChange(SolutionView<Solution_> solutionView, PositionInList targetPosition, Value_ value) {
        var currentPosition = solutionView.getPositionOf(variableMetaModel, value).ensureAssigned();
        if (currentPosition.equals(targetPosition)) { // No change needed.
            return false;
        }

        if (currentPosition.entity() == targetPosition.entity()) { // The value is already in the list.
            var valueCount = solutionView.countValues(variableMetaModel, currentPosition.entity());
            if (valueCount == 1) { // The value is the only value in the list; no change.
                return false;
            } else if (targetPosition.index() == valueCount) { // Trying to move the value past the end of the list.
                return false;
            } else { // Same list, same position; ignore.
                return currentPosition.index() != targetPosition.index();
            }
        }

        // We can move freely between entities, assuming the target entity accepts the value.
        return solutionView.isValueInRange(variableMetaModel, targetPosition.entity(), value);
    }

}
