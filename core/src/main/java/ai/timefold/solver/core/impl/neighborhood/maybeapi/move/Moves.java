package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Moves {

    @SuppressWarnings("unchecked")
    public static <Solution_> Move<Solution_> compose(List<Move<Solution_>> moves) {
        return compose(moves.toArray(new Move[0]));
    }

    @SafeVarargs
    public static <Solution_> Move<Solution_> compose(Move<Solution_>... moves) {
        return CompositeMove.buildMove(moves);
    }

    // ************************************************************************
    // Basic variable moves
    // ************************************************************************

    public static <Solution_, Entity_, Value_> Move<Solution_> change(Entity_ entity, @Nullable Value_ value,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return new ChangeMove<>(variableMetaModel, entity, value);
    }

    public static <Solution_, Entity_> Move<Solution_> swap(Entity_ leftEntity, Entity_ rightEntity,
            PlanningVariableMetaModel<Solution_, Entity_, Object> variableMetaModel) {
        return swap(leftEntity, rightEntity, Collections.singletonList(variableMetaModel));
    }

    public static <Solution_, Entity_> Move<Solution_> swap(Entity_ leftEntity, Entity_ rightEntity,
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList) {
        return new SwapMove<>(variableMetaModelList, leftEntity, rightEntity);
    }

    // ************************************************************************
    // List variable moves
    // ************************************************************************

    public static <Solution_, Entity_, Value_> Move<Solution_> assign(Value_ value, PositionInList targetPosition,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return assign(value, targetPosition.entity(), targetPosition.index(), variableMetaModel);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> assign(Value_ value, Entity_ entity, int index,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return new ListAssignMove<>(variableMetaModel, value, entity, index);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(PositionInList targetPosition,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return unassign(targetPosition.entity(), targetPosition.index(), variableMetaModel);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(Entity_ entity, int index,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return new ListUnassignMove<>(variableMetaModel, entity, index);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> change(PositionInList left, PositionInList right,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return change(left.entity(), left.index(), right.entity(), right.index(), variableMetaModel);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> change(Entity_ leftEntity, int leftIndex, Entity_ rightEntity,
            int rightIndex, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return new ListChangeMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> swap(PositionInList left, PositionInList right,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return swap(left.entity(), left.index(), right.entity(), right.index(), variableMetaModel);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> swap(Entity_ leftEntity, int leftIndex, Entity_ rightEntity,
            int rightIndex, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        return new ListSwapMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    private Moves() {
        // No external instances.
    }

}
