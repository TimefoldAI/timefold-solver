package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.move.streams.dataset.bi.AbstractBiDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.uni.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSessionFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.DataJoiners;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiMoveStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
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

    public DefaultMoveStreamSession<Solution_> createSession(SessionContext<Solution_> context) {
        var session = datasetSessionFactory.buildSession(context);
        return new DefaultMoveStreamSession<>(session, context.solutionView());
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerate(Class<A> sourceClass, boolean includeNull) {
        var entityDescriptor = getSolutionDescriptor().findEntityDescriptor(sourceClass);
        if (entityDescriptor == null) { // Not an entity, can't be pinned.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (entityDescriptor.isGenuine()) { // Genuine entity can be pinned.
            return dataStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
        }
        // From now on, we are testing a shadow entity.
        var listVariableDescriptor = getSolutionDescriptor().getListVariableDescriptor();
        if (listVariableDescriptor == null) { // Can't be pinned when there are only basic variables.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.supportsPinning()) { // The genuine entity does not support pinning.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.acceptsValueType(sourceClass)) { // Can't be used as an element.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        // Finally a valid pin-supporting type.
        return dataStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerateIncludingPinned(Class<A> sourceClass, boolean includeNull) {
        return dataStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
    }

    @Override
    public <Entity_, Value_> BiDataStream<Solution_, Entity_, Value_> enumerateEntityValuePairs(
            GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniDataStream<Solution_, Entity_> entityDataStream) {
        var variableDescriptor = getVariableDescriptor(variableMetaModel);
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        var includeNull =
                variableMetaModel instanceof PlanningVariableMetaModel<Solution_, Entity_, Value_> planningVariableMetaModel
                        ? planningVariableMetaModel.allowsUnassigned()
                        : variableMetaModel instanceof PlanningListVariableMetaModel<Solution_, Entity_, Value_> planningListVariableMetaModel
                                && planningListVariableMetaModel.allowsUnassignedValues();
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            // No need for filtering the value range; all values from solution are valid.
            var stream = dataStreamFactory.forEachFromSolution(variableMetaModel, includeNull);
            return entityDataStream.join(stream);
        } else {
            var stream = dataStreamFactory.forEachExcludingPinned(variableMetaModel.type(), includeNull);
            return entityDataStream.join(stream, DataJoiners.<Solution_, Entity_, Value_> filtering(
                    (solutionView, entity, value) -> solutionView.isValueInRange(variableMetaModel, entity, value)));
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

    @Override
    public <A> UniMoveStream<Solution_, A> pick(UniDataStream<Solution_, A> dataStream) {
        return new DefaultUniMoveStream<>(this,
                ((AbstractUniDataStream<Solution_, A>) dataStream).createDataset());
    }

    @Override
    public <A, B> BiMoveStream<Solution_, A, B> pick(BiDataStream<Solution_, A, B> dataStream) {
        return new DefaultBiFromBiMoveStream<>(((AbstractBiDataStream<Solution_, A, B>) dataStream).createDataset());
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return dataStreamFactory.getSolutionDescriptor();
    }

}
