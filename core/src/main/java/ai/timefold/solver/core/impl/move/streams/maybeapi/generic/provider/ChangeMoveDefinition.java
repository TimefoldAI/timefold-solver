package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveDefinition;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
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
    public MoveProducer<Solution_> build(MoveStreamFactory<Solution_> moveStreamFactory) {
        var dataStream = moveStreamFactory.forEachEntityValuePair(variableMetaModel)
                .filter((solutionView, entity, value) -> {
                    Value_ currentValue = solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity));
                    return !Objects.equals(currentValue, value);
                });
        return moveStreamFactory.pick(dataStream)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, Objects.requireNonNull(entity),
                        value));
    }

}
