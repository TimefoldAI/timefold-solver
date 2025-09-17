package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.definitions;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.maybeapi.DataJoiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.Moves;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveDefinition;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ListSwapMoveDefinition<Solution_, Entity_, Value_>
        implements MoveDefinition<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListSwapMoveDefinition(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveProducer<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var assignedValueStream = moveStreamFactory.forEach(variableMetaModel.type(), false)
                .filter((solutionView,
                        value) -> solutionView.getPositionOf(variableMetaModel, value) instanceof PositionInList);
        var validAssignedValuePairStream = assignedValueStream.join(assignedValueStream,
                DataJoiners.filtering((SolutionView<Solution_> solutionView, Value_ leftValue,
                        Value_ rightValue) -> !Objects.equals(leftValue, rightValue)));
        // Ensure unique pairs; without demanding PlanningId, this becomes tricky.
        // Convert values to their locations in list.
        var validAssignedValueUniquePairStream =
                validAssignedValuePairStream
                        .map((solutionView, leftValue, rightValue) -> new UniquePair<>(leftValue, rightValue))
                        .distinct()
                        .map((solutionView, pair) -> FullElementPosition.of(variableMetaModel, solutionView, pair.first()),
                                (solutionView, pair) -> FullElementPosition.of(variableMetaModel, solutionView, pair.second()));
        // Eliminate pairs that cannot be swapped due to value range restrictions.
        var result = validAssignedValueUniquePairStream
                .filter((solutionView, leftPosition, rightPosition) -> solutionView.isValueInRange(variableMetaModel,
                        rightPosition.entity(), leftPosition.value())
                        && solutionView.isValueInRange(variableMetaModel, leftPosition.entity(), rightPosition.value()));
        // Finally pick the moves.
        return moveStreamFactory.pick(result)
                .asMove((solutionView, leftPosition, rightPosition) -> Moves.swap(leftPosition.elementPosition,
                        rightPosition.elementPosition, variableMetaModel));
    }

    @NullMarked
    private record FullElementPosition<Entity_, Value_>(Value_ value, PositionInList elementPosition) {

        public static <Solution_, Entity_, Value_> FullElementPosition<Entity_, Value_> of(
                PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
                SolutionView<Solution_> solutionView, Value_ value) {
            var assignedElement = solutionView.getPositionOf(variableMetaModel, value).ensureAssigned();
            return new FullElementPosition<>(value, assignedElement);
        }

        public Entity_ entity() {
            return elementPosition.entity();
        }

        public int index() {
            return elementPosition.index();
        }

    }

}
