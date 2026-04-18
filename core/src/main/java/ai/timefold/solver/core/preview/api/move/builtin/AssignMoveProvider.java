package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

/**
 * Assigns a non-null value to each entity whose planning variable is currently unassigned (null).
 * Only generates moves where the value is in range for that entity.
 * <p>
 * For moving an already-assigned entity to a different non-null value, see {@link ChangeMoveProvider}.
 * For moving an already-assigned entity to null (unassigning), see {@code UnassignMoveProvider}.
 * <p>
 * Requires that the variable {@link PlanningVariableMetaModel#allowsUnassigned() allows unassigned};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 */
@NullMarked
public final class AssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public AssignMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassigned()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var nodeSharingSupportFunctions = ((DefaultMoveStreamFactory<Solution_>) moveStreamFactory)
                .getNodeSharingSupportFunctions(variableMetaModel);
        var unassignedEntities = moveStreamFactory.forEach(variableMetaModel.entity().type(), false)
                .filter((view, e) -> view.getValue(variableMetaModel, e) == null);
        return moveStreamFactory.pick(unassignedEntities)
                .pick(moveStreamFactory.forEach(variableMetaModel.type(), false),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilter()))
                .asMove((view, entity, value) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), value));
    }

}
