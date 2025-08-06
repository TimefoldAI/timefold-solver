package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var dataStream = moveStreamFactory.enumerateEntityValuePairs(variableMetaModel)
                .filter((solutionView, entity, value) -> {
                    Value_ currentValue = solutionView.getValue(variableMetaModel, Objects.requireNonNull(entity));
                    return !Objects.equals(currentValue, value);
                });
        return moveStreamFactory.pick(dataStream)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, Objects.requireNonNull(entity),
                        value));
    }

}
