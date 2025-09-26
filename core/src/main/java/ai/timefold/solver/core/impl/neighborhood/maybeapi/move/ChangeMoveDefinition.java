package ai.timefold.solver.core.impl.neighborhood.maybeapi.move;

import java.util.Objects;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ChangeMoveDefinition<Solution_, Entity_, Value_>
        implements MoveDefinition<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveDefinition(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveStream<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var enumeratingStream =
                moveStreamFactory.forEachEntityValuePair(variableMetaModel)
                        .filter((solutionView, entity, value) -> {
                            Value_ currentValue = solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity));
                            return !Objects.equals(currentValue, value);
                        });
        return moveStreamFactory.pick(enumeratingStream)
                .asMove((solution, entity, value) -> Moves.change(Objects.requireNonNull(entity), value, variableMetaModel));
    }

}
