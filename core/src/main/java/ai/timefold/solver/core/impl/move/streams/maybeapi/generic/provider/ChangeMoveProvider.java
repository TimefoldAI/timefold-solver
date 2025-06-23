package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import java.util.Objects;

import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;
import ai.timefold.solver.core.impl.move.streams.pickers.FilteringBiPicker;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final BiPicker<Entity_, @Nullable Value_> picker;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.picker = new FilteringBiPicker<Solution_, Entity_, @Nullable Value_>((solutionView, entity, value) -> {
            Value_ oldValue = solutionView.getValue(variableMetaModel, entity);
            if (Objects.equals(oldValue, value)) {
                return false;
            }
            return solutionView.isValueInRange(variableMetaModel, entity, value);
        });
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var defaultMoveStreamFactory = (DefaultMoveStreamFactory<Solution_>) moveStreamFactory;
        var entityStream = defaultMoveStreamFactory.enumerate(variableMetaModel.entity().type());
        var valueStream = defaultMoveStreamFactory.enumerate(variableMetaModel.type());
        return moveStreamFactory.pick(entityStream)
                .pick(valueStream, picker)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, entity, value));
    }

}
