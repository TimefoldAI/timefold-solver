package ai.timefold.solver.core.preview.api.move.builtin;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.jspecify.annotations.NullMarked;

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
        return moveStreamFactory.pick(moveStreamFactory.forEach(variableMetaModel.entity().type(), false))
                .pick(moveStreamFactory.forEach(variableMetaModel.type(), variableMetaModel.allowsUnassigned()),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.differentValueFilter()),
                        NeighborhoodsJoiners.filtering(nodeSharingSupportFunctions.valueInRangeFilter()))
                .asMove((solution, entity, value) -> Moves.change(variableMetaModel, Objects.requireNonNull(entity), value));
    }

}
