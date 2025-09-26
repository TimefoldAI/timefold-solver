package ai.timefold.solver.core.impl.neighborhood.stream;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.EnumeratingJoiners;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.BiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling.UniSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.AbstractBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultBiFromBiSamplingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultUniSamplingStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.preview.api.domain.metamodel.GenuineVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultMoveStreamFactory<Solution_>
        implements MoveStreamFactory<Solution_> {

    private final EnumeratingStreamFactory<Solution_> enumeratingStreamFactory;
    private final DatasetSessionFactory<Solution_> datasetSessionFactory;

    public DefaultMoveStreamFactory(SolutionDescriptor<Solution_> solutionDescriptor, EnvironmentMode environmentMode) {
        this.enumeratingStreamFactory = new EnumeratingStreamFactory<>(solutionDescriptor, environmentMode);
        this.datasetSessionFactory = new DatasetSessionFactory<>(enumeratingStreamFactory);
    }

    public DefaultNeighborhoodSession<Solution_> createSession(SessionContext<Solution_> context) {
        var session = datasetSessionFactory.buildSession(context);
        return new DefaultNeighborhoodSession<>(session, context.solutionView());
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return enumeratingStreamFactory.getSolutionDescriptor().getMetaModel();
    }

    @Override
    public <A> UniEnumeratingStream<Solution_, A> forEach(Class<A> sourceClass, boolean includeNull) {
        var entityDescriptor = getSolutionDescriptor().findEntityDescriptor(sourceClass);
        if (entityDescriptor == null) { // Not an entity, can't be pinned.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (entityDescriptor.isGenuine()) { // Genuine entity can be pinned.
            return enumeratingStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
        }
        // From now on, we are testing a shadow entity.
        var listVariableDescriptor = getSolutionDescriptor().getListVariableDescriptor();
        if (listVariableDescriptor == null) { // Can't be pinned when there are only basic variables.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.supportsPinning()) { // The genuine entity does not support pinning.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        if (!listVariableDescriptor.acceptsValueType(sourceClass)) { // Can't be used as an element.
            return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
        }
        // Finally a valid pin-supporting type.
        return enumeratingStreamFactory.forEachExcludingPinned(sourceClass, includeNull);
    }

    @Override
    public <A> UniEnumeratingStream<Solution_, A> forEachUnfiltered(Class<A> sourceClass, boolean includeNull) {
        return enumeratingStreamFactory.forEachNonDiscriminating(sourceClass, includeNull);
    }

    @Override
    public <Entity_, Value_> BiEnumeratingStream<Solution_, Entity_, Value_> forEachEntityValuePair(
            GenuineVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            UniEnumeratingStream<Solution_, Entity_> entityEnumeratingStream) {
        var includeNull =
                variableMetaModel instanceof PlanningVariableMetaModel<Solution_, Entity_, Value_> planningVariableMetaModel
                        ? planningVariableMetaModel.allowsUnassigned()
                        : variableMetaModel instanceof PlanningListVariableMetaModel<Solution_, Entity_, Value_> planningListVariableMetaModel
                                && planningListVariableMetaModel.allowsUnassignedValues();
        var stream = enumeratingStreamFactory.forEachExcludingPinned(variableMetaModel.type(), includeNull);
        return entityEnumeratingStream.join(stream, EnumeratingJoiners.<Solution_, Entity_, Value_> filtering(
                (solutionView, entity, value) -> solutionView.isValueInRange(variableMetaModel, entity, value)));
    }

    @Override
    public <A> UniSamplingStream<Solution_, A> pick(UniEnumeratingStream<Solution_, A> enumeratingStream) {
        return new DefaultUniSamplingStream<>(((AbstractUniEnumeratingStream<Solution_, A>) enumeratingStream).createDataset());
    }

    @Override
    public <A, B> BiSamplingStream<Solution_, A, B> pick(BiEnumeratingStream<Solution_, A, B> enumeratingStream) {
        return new DefaultBiFromBiSamplingStream<>(
                ((AbstractBiEnumeratingStream<Solution_, A, B>) enumeratingStream).createDataset());
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return enumeratingStreamFactory.getSolutionDescriptor();
    }

}
