package ai.timefold.solver.core.impl.move.streams.generic.provider;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.move.streams.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.generic.move.ChangeMove;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProducer;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveProvider;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ChangeMoveProvider<Solution_, Entity_, Value_>
        implements MoveProvider<Solution_> {

    private final PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel;
    private final Predicate<@Nullable Value_> valueFilter;
    private final BiPredicate<Entity_, @Nullable Value_> entityAndValueFilter;

    public ChangeMoveProvider(PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel) {
        this.variableMetaModel = Objects.requireNonNull(variableMetaModel);
        var variableDescriptor = ((DefaultPlanningVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel)
                .variableDescriptor();
        this.valueFilter = variableMetaModel.allowsUnassigned() ? value -> true : Objects::nonNull;
        this.entityAndValueFilter = (entity, value) -> variableDescriptor.getValue(entity) != value;
    }

    @Override
    public MoveProducer<Solution_> apply(MoveStreamFactory<Solution_> moveStreamFactory) {
        var defaultMoveStreamFactory = (DefaultMoveStreamFactory<Solution_>) moveStreamFactory;
        var entityStream = defaultMoveStreamFactory.enumerate(variableMetaModel.entity().type())
                .filter(this::acceptEntity);
        var valueStream = defaultMoveStreamFactory.enumeratePossibleValues(variableMetaModel)
                .filter(this::acceptValue);
        return moveStreamFactory.pick(entityStream)
                .pick(valueStream, this::acceptEntityValuePair)
                .asMove((solution, entity, value) -> new ChangeMove<>(variableMetaModel, entity, value));
    }

    /**
     * Determines whether the given entity should be accepted to produce a move.
     *
     * @param entity the entity to evaluate
     * @return {@code true} if the entity is accepted, {@code false} otherwise; defaults to true.
     */
    protected boolean acceptEntity(Entity_ entity) {
        return true;
    }

    /**
     * Evaluates whether the given value (from the applicable value range) should be accepted to produce a move.
     *
     * @param value the value to evaluate
     * @return {@code true} if the value is accepted, {@code false} otherwise;
     *         by default, it rejects null values if the {@link PlanningVariable#allowsUnassigned()} is false.
     */
    protected boolean acceptValue(@Nullable Value_ value) {
        return valueFilter.test(value);
    }

    /**
     * Determines whether a given entity and value pair should be accepted to produce a move.
     *
     * @param entity the entity to evaluate, already accepted by {@link #acceptEntity(Object)}
     * @param value the value to evaluate, already accepted by {@link #acceptValue(Object)}
     * @return {@code true} if the entity-value pair is accepted, {@code false} otherwise;
     *         by default, the pair is accepted if the entity's value is different from the value to evaluate.
     */
    protected boolean acceptEntityValuePair(Entity_ entity, @Nullable Value_ value) {
        return entityAndValueFilter.test(entity, value);
    }

}
