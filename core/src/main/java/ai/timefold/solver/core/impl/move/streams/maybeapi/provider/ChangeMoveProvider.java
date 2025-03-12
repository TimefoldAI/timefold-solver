package ai.timefold.solver.core.impl.move.streams.maybeapi.provider;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.generic.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveConstructor;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveConstructor<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var valueStream = moveStreamFactory.enumeratePossibleValues(variableMetaModel)
                .filter(this::acceptValue);
        if (variableMetaModel.allowsUnassigned()) {
            valueStream = valueStream.addNull();
        }
        return moveStreamFactory.pick(moveStreamFactory.enumerateEntities(variableMetaModel.entity())
                .filter(this::acceptEntity))
                .pick(valueStream, this::acceptEntityValuePair)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, entity, value));
    }

    protected boolean acceptEntity(Entity_ entity) {
        return true;
    }

    protected boolean acceptValue(Value_ value) {
        return true;
    }

    protected boolean acceptEntityValuePair(Entity_ entity, Value_ value) {
        return true;
    }

}
