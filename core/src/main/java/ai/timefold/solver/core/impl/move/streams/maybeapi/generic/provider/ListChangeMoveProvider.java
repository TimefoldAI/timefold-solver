package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.DataJoiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListAssignMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ListUnassignMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public class ListChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final BiDataFilter<Solution_, Entity_, Value_> isValueInListFilter;
    private final BiDataFilter<Solution_, Value_, ElementPosition> validChangeFilter;

    public ListChangeMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.isValueInListFilter = (solution, entity, value) -> {
            if (entity == null || value == null) {
                // Necessary for the null to survive until the later stage,
                // where we will use it as a marker to unassign the value.
                return true;
            }
            return solution.isValueInRange(variableMetaModel, entity, value);
        };
        this.validChangeFilter = (solutionView, value, targetPosition) -> {
            var currentPosition = solutionView.getPositionOf(variableMetaModel, value);
            if (currentPosition.equals(targetPosition)) {
                return false;
            }
            if (currentPosition instanceof UnassignedElement) {
                var targetPositionInList = targetPosition.ensureAssigned();
                return solutionView.isValueInRange(variableMetaModel, targetPositionInList.entity(), value);
            } else {
                if (!(targetPosition instanceof PositionInList targetPositionInList)) { // Unassigning a value.
                    return true;
                }
                var currentPositionInList = currentPosition.ensureAssigned();
                if (currentPositionInList.entity() == targetPositionInList.entity()) {
                    var valueCount = solutionView.countValues(variableMetaModel, currentPositionInList.entity());
                    if (valueCount == 1) {
                        return false; // The value is already in the list, and it is the only one.
                    } else {
                        return currentPositionInList.index() != targetPositionInList.index();
                    }
                } else {
                    // We can move freely between entities.
                    return solutionView.isValueInRange(variableMetaModel, targetPositionInList.entity(), value);
                }
            }
        };
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        // For each unassigned value, we need to create a move to assign it to same position of some list variable.
        // For each assigned value that is not pinned, we need to create:
        // - A move to unassign it.
        // - A move to reassign it to another position if assigned.
        // To assign or reassign a value, we need to create:
        // - A move for every unpinned value in every entity's list variable to assign the value before that position.
        // - A move for every entity to assign it to the last position in the list variable.
        var unpinnedEntities = moveStreamFactory.enumerate(variableMetaModel.entity().type(), false);
        var unpinnedValues = moveStreamFactory.enumerate(variableMetaModel.type(), true)
                .filter((solutionView, value) -> value == null || solutionView.getPositionOf(variableMetaModel, value) instanceof PositionInList);
        var entityValuePairs = unpinnedEntities.join(unpinnedValues, DataJoiners.filtering(isValueInListFilter))
                .map((solutionView, entity, value) -> {
                    var valueCount = solutionView.countValues(variableMetaModel, entity);
                    if (value == null || valueCount == 0) { // This will trigger assignment of the value at the end of the list.
                        return ElementPosition.of(entity, valueCount);
                    } else { // This will trigger assignment of the value immediately before this value.
                        return solutionView.getPositionOf(variableMetaModel, value);
                    }
                })
                .distinct();
        var dataStream = moveStreamFactory.enumerate(variableMetaModel.type(), false)
                .join(entityValuePairs, DataJoiners.filtering(validChangeFilter));
        return moveStreamFactory.pick(dataStream)
                .asMove((solutionView, value, targetPosition) -> {
                    var currentPosition = solutionView.getPositionOf(variableMetaModel, Objects.requireNonNull(value));
                    if (targetPosition instanceof UnassignedElement) {
                        var currentElementPosition = currentPosition.ensureAssigned();
                        return new ListUnassignMove<>(variableMetaModel, value, currentElementPosition.entity(),
                                currentElementPosition.index());
                    }
                    var targetElementPosition = Objects.requireNonNull(targetPosition).ensureAssigned();
                    if (currentPosition instanceof UnassignedElement) {
                        return new ListAssignMove<>(variableMetaModel, value, targetElementPosition.entity(),
                                targetElementPosition.index());
                    }
                    var currentElementPosition = currentPosition.ensureAssigned();
                    return new ListChangeMove<>(variableMetaModel, currentElementPosition.entity(),
                            currentElementPosition.index(), targetElementPosition.entity(), targetElementPosition.index());
                });
    }

}
