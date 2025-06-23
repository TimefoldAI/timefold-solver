package ai.timefold.solver.core.impl.move.streams;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.move.streams.dataset.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.DatasetSessionFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.MoveStreamFactory;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniMoveStream;

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

    public DefaultMoveStreamSession<Solution_> createSession(Solution_ workingSolution, SupplyManager supplyManager) {
        var session = datasetSessionFactory.buildSession();
        session.initialize(workingSolution, supplyManager);
        return new DefaultMoveStreamSession<>(session, workingSolution);
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerate(Class<A> sourceClass) {
        var entityDescriptor = getSolutionDescriptor().findEntityDescriptor(sourceClass);
        if (entityDescriptor == null) { // Not an entity, can't be pinned.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass);
        }
        if (entityDescriptor.isGenuine()) { // Genuine entity can be pinned.
            return dataStreamFactory.forEachExcludingPinned(sourceClass);
        }
        // From now on, we are testing a shadow entity.
        var listVariableDescriptor = getSolutionDescriptor().getListVariableDescriptor();
        if (listVariableDescriptor == null) { // Can't be pinned when there are only basic variables.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass);
        }
        if (!listVariableDescriptor.supportsPinning()) { // The genuine entity does not support pinning.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass);
        }
        if (!listVariableDescriptor.acceptsValueType(sourceClass)) { // Can't be used as an element.
            return dataStreamFactory.forEachNonDiscriminating(sourceClass);
        }
        // Finally a valid pin-supporting type.
        return dataStreamFactory.forEachExcludingPinned(sourceClass);
    }

    @Override
    public <A> UniDataStream<Solution_, A> enumerateIncludingPinned(Class<A> sourceClass) {
        return dataStreamFactory.forEachNonDiscriminating(sourceClass);
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
