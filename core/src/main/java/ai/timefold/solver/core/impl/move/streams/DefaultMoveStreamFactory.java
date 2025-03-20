package ai.timefold.solver.core.impl.move.streams;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSessionFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamFactory<Solution_>
        implements MoveStreamFactory<Solution_> {

    private final DataStreamFactory<Solution_> dataStreamFactory;
    private final DatasetSessionFactory<Solution_> datasetSessionFactory;

    public DefaultMoveStreamFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.dataStreamFactory = new DataStreamFactory<>(solutionDescriptor);
        this.datasetSessionFactory = new DatasetSessionFactory<>(dataStreamFactory);
    }

    public DefaultMoveStreamSession<Solution_> createSession(Solution_ workingSolution) {
        var session = datasetSessionFactory.buildSession();
        session.initialize(workingSolution);
        return new DefaultMoveStreamSession<>(this, session, workingSolution);
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerate(Class<A> clz) {
        return dataStreamFactory.forEachIncludingUnassigned(clz);
    }

    public <Entity_> UniDataStream<Solution_, Entity_>
            enumerateEntities(PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        return enumerate(entityMetaModel.type());
    }

    public <Entity_, A> UniDataStream<Solution_, A>
            enumeratePossibleValues(GenuineVariableMetaModel<Solution_, Entity_, A> variableMetaModel) {
        if (variableMetaModel instanceof DefaultPlanningVariableMetaModel<Solution_, Entity_, A> planningVariableMetaModel) {
            var variableDescriptor = planningVariableMetaModel.variableDescriptor();
            var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
            if (variableDescriptor.isValueRangeEntityIndependent()) {
                return enumerate(new FromSolutionValueCollectingFunction<>(valueRangeDescriptor));
            } else {
                return enumerateFromEntity(variableMetaModel.entity(),
                        ((solution, entity) -> ensureCountable(valueRangeDescriptor.extractValueRange(solution, entity))));
            }
        } else if (variableMetaModel instanceof DefaultPlanningListVariableMetaModel<Solution_, Entity_, A> planningListVariableMetaModel) {
            var variableDescriptor = planningListVariableMetaModel.variableDescriptor();
            var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
            if (variableDescriptor.isValueRangeEntityIndependent()) {
                return enumerate(new FromSolutionValueCollectingFunction<>(valueRangeDescriptor));
            } else { // TODO enable this
                throw new UnsupportedOperationException("List variable value range on entity is not yet supported.");
            }
        } else {
            throw new IllegalStateException(
                    "Impossible state: variable metamodel (%s) represents neither basic not list variable."
                            .formatted(variableMetaModel.getClass().getSimpleName()));
        }
    }

    private static <A> CountableValueRange<A> ensureCountable(ValueRange<A> valueRange) {
        if (valueRange instanceof CountableValueRange<A> countableValueRange) {
            return countableValueRange;
        } else { // Non-countable value ranges cannot be enumerated.
            throw new UnsupportedOperationException("The value range (%s) is not countable."
                    .formatted(valueRange));
        }
    }

    private <A> UniDataStream<Solution_, A>
            enumerate(FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction) {
        return dataStreamFactory.forEach(valueCollectingFunction);
    }

    private <Entity_, A> UniDataStream<Solution_, A> enumerateFromEntity(
            PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel,
            BiFunction<Solution_, Entity_, CountableValueRange<A>> collectionFunction) {
        return null;
    }

    @Override
    public <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream) {
        return new DefaultUniMoveStream<>(this,
                ((AbstractUniDataStream<Solution_, A>) dataStream).createDataset());
    }

}
