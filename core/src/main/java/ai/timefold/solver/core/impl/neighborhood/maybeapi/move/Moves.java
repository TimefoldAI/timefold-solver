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

    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity, @Nullable Value_ value) {
        return new ChangeMove<>(variableMetaModel, entity, value);
    }

    public static <Solution_, Entity_> Move<Solution_> swap(
            PlanningVariableMetaModel<Solution_, Entity_, Object> variableMetaModel, Entity_ leftEntity, Entity_ rightEntity) {
        return swap(Collections.singletonList(variableMetaModel), leftEntity, rightEntity);
    }

    public static <Solution_, Entity_> Move<Solution_> swap(
            List<PlanningVariableMetaModel<Solution_, Entity_, Object>> variableMetaModelList, Entity_ leftEntity,
            Entity_ rightEntity) {
        return new SwapMove<>(variableMetaModelList, leftEntity, rightEntity);
    }

    // ************************************************************************
    // List variable moves
    // ************************************************************************

    public static <Solution_, Entity_, Value_> Move<Solution_> assign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value,
            PositionInList targetPosition) {
        return assign(variableMetaModel, value, targetPosition.entity(), targetPosition.index());
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> assign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Value_ value, Entity_ entity,
            int index) {
        return new ListAssignMove<>(variableMetaModel, value, entity, index);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList targetPosition) {
        return unassign(targetPosition.entity(), variableMetaModel, targetPosition.index());
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> unassign(Entity_ entity,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, int index) {
        return new ListUnassignMove<>(variableMetaModel, entity, index);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList left,
            PositionInList right) {
        return change(variableMetaModel, left.entity(), left.index(), right.entity(), right.index());
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> change(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        return new ListChangeMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, PositionInList left,
            PositionInList right) {
        return swap(variableMetaModel, left.entity(), left.index(), right.entity(), right.index());
    }

    public static <Solution_, Entity_, Value_> Move<Solution_> swap(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ leftEntity, int leftIndex,
            Entity_ rightEntity, int rightIndex) {
        return new ListSwapMove<>(variableMetaModel, leftEntity, leftIndex, rightEntity, rightIndex);
    }

    private Moves() {
        // No external instances.
    }

}
