package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

/**
 * Assigns each unassigned value to a position in some entity's list variable.
 * Only generates moves where the value is in range for the destination entity.
 * <p>
 * For moving an already-assigned value to a different position, see {@link ListChangeMoveProvider}.
 * For removing an already-assigned value from its position (unassigning), see {@code ListUnassignMoveProvider}.
 * <p>
 * Requires that the variable {@link PlanningListVariableMetaModel#allowsUnassignedValues() allows unassigned values};
 * otherwise the constructor throws {@link IllegalArgumentException}.
 */
@NullMarked
public final class ListAssignMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ListAssignMoveProvider(PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        if (!variableMetaModel.allowsUnassignedValues()) {
            throw new IllegalArgumentException(
                    "The variableMetaModel (%s) must allow unassigned values, but it does not."
                            .formatted(variableMetaModel));
        }
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var nodeSharingSupportFunctions = ((DefaultMoveStreamFactory<Solution_>) moveStreamFactory)
                .getNodeSharingSupportFunctions(variableMetaModel);
        return moveStreamFactory.pick(moveStreamFactory.forEachUnassignedValue(variableMetaModel))
                .pick(moveStreamFactory.forEachDestination(variableMetaModel),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilterForPosition()))
                .asMove((view, value, pos) -> Moves.assign(variableMetaModel, Objects.requireNonNull(value), pos));
    }

}
