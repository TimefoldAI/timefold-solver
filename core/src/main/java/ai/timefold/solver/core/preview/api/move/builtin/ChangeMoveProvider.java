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
 * For each entity with a non-null value, creates a move to assign it a different non-null value.
 * Null-to-non-null (assign) moves are handled by {@code AssignMoveProvider}.
 * Non-null-to-null (unassign) moves are handled by {@code UnassignMoveProvider}.
 */
@NullMarked
public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var nodeSharingSupportFunctions =
                ((DefaultMoveStreamFactory<Solution_>) moveStreamFactory).getNodeSharingSupportFunctions(variableMetaModel);
        var entities = moveStreamFactory.forEach(variableMetaModel.entity().type(), false);
        if (variableMetaModel.allowsUnassigned()) {
            entities = entities.filter(nodeSharingSupportFunctions.assignedValueFilter());
        }
        return moveStreamFactory.pick(entities)
                .pick(moveStreamFactory.forEach(variableMetaModel.type(), false),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.differentValueFilter()),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilter()))
                .asMove((solution, entity, value) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), value));
    }

}
