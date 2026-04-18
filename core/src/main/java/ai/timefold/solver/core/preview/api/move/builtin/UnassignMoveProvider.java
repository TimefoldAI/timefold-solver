package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * For each entity whose basic planning variable is currently assigned (non-null),
 * creates a move to unassign it (set the variable to null).
 * <p>
 * This provider only applies to planning variables that allow unassigned values.
 * For the complementary moves:
 * <ul>
 * <li>Use {@link AssignMoveProvider} to assign a value to currently-unassigned entities.</li>
 * <li>Use {@link ChangeMoveProvider} to change an entity's value to a different non-null value.</li>
 * </ul>
 * <p>
 * <strong>This class is part of the Neighborhoods API, which is under development and is only offered as a preview
 * feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 */
@NullMarked
public class UnassignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public UnassignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        return moveStreamFactory.pick(
                moveStreamFactory.forEach(variableMetaModel.entity().type(), false)
                        .filter((view, e) -> view.getValue(variableMetaModel, e) != null))
                .asMove((view, entity) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), null));
    }

}
