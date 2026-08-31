package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ListSwapMoveProvider<Solution_, Entity_, Value_> implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListSwapMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        // Unassigned values are admitted only when the variable allows them:
        // the one-side-unassigned case below composes unassign+assign, which would otherwise
        // leave a value unassigned on a variable that forbids it.
        var valueStream = (variableMetaModel.allowsUnassignedValues()
                ? moveStreamFactory.forEach(variableMetaModel.type(), false)
                : moveStreamFactory.forEachAssignedValue(variableMetaModel))
                .map((solutionView, value) -> new FullElementPosition<>(value,
                        solutionView.getPositionOf(variableMetaModel, value)));
        var predicate =
                (BiNeighborhoodsPredicate<Solution_, FullElementPosition<Value_>, FullElementPosition<Value_>>) this::isValidSwap;
        // We do not exclude duplicate swaps (A<>B and B<>A) to keep it simple and fast.
        // Move selectors don't do anything about duplicate moves either.
        return moveStreamFactory.pick(valueStream)
                .pick(valueStream,
                        NeighborhoodsJoiners.filtering(predicate))
                .asMove(this::buildMove);
    }

    private Move<Solution_> buildMove(SolutionView<Solution_> solutionView, FullElementPosition<Value_> a,
            FullElementPosition<Value_> b) {
        if (a.elementPosition() instanceof PositionInList aPosition
                && b.elementPosition() instanceof PositionInList bPosition) {
            return Moves.swap(variableMetaModel, aPosition, bPosition);
        }
        // Exactly one side is unassigned (isValidSwap already excluded both-unassigned):
        // unassign the assigned side, then assign the incoming value at the same index.
        var assignedPosition = (PositionInList) (a.elementPosition() instanceof PositionInList
                ? a.elementPosition()
                : b.elementPosition());
        var incomingValue = a.elementPosition() instanceof PositionInList ? b.value() : a.value();
        return Moves.compose(
                Moves.unassign(variableMetaModel, assignedPosition),
                Moves.assign(variableMetaModel, incomingValue, assignedPosition));
    }

    private boolean isValidSwap(SolutionView<Solution_> solutionView, FullElementPosition<Value_> leftPosition,
            FullElementPosition<Value_> rightPosition) {
        if (Objects.equals(leftPosition, rightPosition)) {
            return false;
        }
        var left = leftPosition.elementPosition();
        var right = rightPosition.elementPosition();
        if (left instanceof PositionInList leftAssigned && right instanceof PositionInList rightAssigned) {
            return solutionView.isValueInRange(variableMetaModel, rightAssigned.entity(), leftPosition.value())
                    && solutionView.isValueInRange(variableMetaModel, leftAssigned.entity(), rightPosition.value());
        } else if (left instanceof PositionInList leftAssigned) {
            return solutionView.isValueInRange(variableMetaModel, leftAssigned.entity(), rightPosition.value());
        } else if (right instanceof PositionInList rightAssigned) {
            return solutionView.isValueInRange(variableMetaModel, rightAssigned.entity(), leftPosition.value());
        } else {
            return false; // Both unassigned: a no-op, never emitted.
        }
    }

    @NullMarked
    private record FullElementPosition<Value_>(Value_ value, ElementPosition elementPosition) {

        @Override
        public String toString() {
            return value + "@" + elementPosition;
        }

    }

}
