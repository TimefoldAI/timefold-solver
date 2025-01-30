package ai.timefold.solver.core.preview.api.move.provider;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.generic.ChangeMove;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.MoveConstructor;
import ai.timefold.solver.core.preview.api.move.stream.MoveProvider;
import ai.timefold.solver.core.preview.api.move.stream.MoveStreams;

public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveConstructor<Solution_> apply(MoveStreams<Solution_> solutionMoveStreams) {
        var valueStream = solutionMoveStreams.enumeratePossibleValues(variableMetaModel)
                .filter(this::acceptValue);
        if (variableMetaModel.allowsUnassigned()) {
            valueStream = valueStream.addNull();
        }
        return solutionMoveStreams.pick(solutionMoveStreams.enumerateEntities(variableMetaModel.entity())
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
