package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var defaultMoveStreamFactory = (DefaultMoveStreamFactory<Solution_>) moveStreamFactory;
        var variableDescriptor = ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel)
                .variableDescriptor();
        var dataStream = defaultMoveStreamFactory.enumerateEntityValuePairs(variableMetaModel)
                .filter((entity, value) -> {
                    var currentValue = variableDescriptor.getValue(entity);
                    return !Objects.equals(currentValue, value);
                });
        return defaultMoveStreamFactory.pick(dataStream)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, entity, value));
    }

}
