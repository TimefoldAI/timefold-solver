package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSessionFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

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
        return new DefaultMoveStreamSession<>(session, workingSolution);
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerate(Class<A> clz) {
        return dataStreamFactory.forEachIncludingUnassigned(clz);
    }

    public <Entity_> UniDataStream<Solution_, Entity_>
            enumerateEntities(PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel) {
        return enumerate(entityMetaModel.type());
    }

    public <Entity_> UniDataStream<Solution_, Entity_>
            enumerateEntities(PlanningVariableMetaModel<Solution_, Entity_, ?> variableMetaModel) {
        return enumerateEntities(variableMetaModel.entity());
    }

    /**
     * Enumerate possible values for a given variable.
     * If the variable allows unassigned values, the resulting stream will include a null value.
     *
     * @return data stream with all possible values of a given variable
     */
    public <Entity_, A> UniDataStream<Solution_, A>
            enumeratePossibleValues(GenuineVariableMetaModel<Solution_, Entity_, A> variableMetaModel) {
        var variableDescriptor = getVariableDescriptor(variableMetaModel);
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        if (variableDescriptor.isValueRangeEntityIndependent()) {
            return enumerate(new FromSolutionValueCollectingFunction<>(valueRangeDescriptor));
        } else {
            throw new UnsupportedOperationException("Value range on entity is not yet supported.");
        }
    }

    private static <Solution_> GenuineVariableDescriptor<Solution_>
            getVariableDescriptor(GenuineVariableMetaModel<Solution_, ?, ?> variableMetaModel) {
        if (variableMetaModel instanceof DefaultPlanningVariableMetaModel<Solution_, ?, ?> planningVariableMetaModel) {
            return planningVariableMetaModel.variableDescriptor();
        } else if (variableMetaModel instanceof DefaultPlanningListVariableMetaModel<Solution_, ?, ?> planningListVariableMetaModel) {
            return planningListVariableMetaModel.variableDescriptor();
        } else {
            throw new IllegalStateException(
                    "Impossible state: variable metamodel (%s) represents neither basic not list variable."
                            .formatted(variableMetaModel.getClass().getSimpleName()));
        }
    }

    private <A> UniDataStream<Solution_, A>
            enumerate(FromSolutionValueCollectingFunction<Solution_, A> valueCollectingFunction) {
        return dataStreamFactory.forEach(valueCollectingFunction);
    }

    @Override
    public <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream) {
        return new DefaultUniMoveStream<>(this,
                ((AbstractUniDataStream<Solution_, A>) dataStream).createDataset());
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return dataStreamFactory.getSolutionDescriptor();
    }

}
