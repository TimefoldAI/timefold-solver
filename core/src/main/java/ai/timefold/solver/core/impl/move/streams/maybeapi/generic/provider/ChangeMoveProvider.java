package ai.timefold.solver.core.impl.move.streams.maybeapi.generic.provider;

import java.util.Objects;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.generic.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;
import ai.timefold.solver.core.impl.move.streams.pickers.FilteringBiPicker;
import ai.timefold.solver.core.impl.move.streams.pickers.SolutionBasedFilteringBiPicker;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final @Nullable Predicate<@Nullable Value_> valueFilter;
    private final BiPicker<Entity_, @Nullable Value_> picker;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        this.valueFilter = variableMetaModel.allowsUnassigned() ? null : Objects::nonNull;
        var variableDescriptor = ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel)
                .variableDescriptor();
        var basePicker = new FilteringBiPicker<Entity_, Value_>((entity, value) -> {
            var oldValue = variableDescriptor.getValue(entity);
            return !Objects.equals(oldValue, value);
        });
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        this.picker = variableMetaModel.hasValueRangeOnEntity()
                ? SolutionBasedFilteringBiPicker.<Solution_, Entity_, Value_> wrap(basePicker,
                        (solution, entity, value) -> {
                            // TODO Optimize this by caching the result when possible.
                            var valueRange = valueRangeDescriptor.extractValueRange(solution, entity);
                            return valueRange.contains(value);
                        })
                : basePicker;
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var defaultMoveStreamFactory = (DefaultMoveStreamFactory<Solution_>) moveStreamFactory;
        var entityStream = defaultMoveStreamFactory.enumerate(variableMetaModel.entity().type());
        var valueStream = defaultMoveStreamFactory.enumerate(variableMetaModel.type());
        if (valueFilter != null) {
            valueStream = valueStream.filter(valueFilter);
        }
        return moveStreamFactory.pick(entityStream)
                .pick(valueStream, picker)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, entity, value));
    }

}
