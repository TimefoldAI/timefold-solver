package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * For each value currently assigned to any entity's list variable,
 * creates a move to unassign it (remove it from the list).
 * <p>
 * This provider only applies to list variables that allow unassigned values.
 * For the complementary moves:
 * <ul>
 * <li>Use {@link ListAssignMoveProvider} to assign currently-unassigned values to list positions.</li>
 * <li>Use {@link ListChangeMoveProvider} to move an assigned value to a different position.</li>
 * </ul>
 * <p>
 * <strong>This class is part of the Neighborhoods API, which is under development and is only offered as a preview
 * feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 */
@NullMarked
public class ListUnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListUnassignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        return moveStreamFactory.pick(moveStreamFactory.forEachAssignedValue(variableMetaModel))
                .asMove((view, value) -> Moves.unassign(variableMetaModel,
                        view.getPositionOf(variableMetaModel, Objects.requireNonNull(value)).ensureAssigned()));
    }

}
