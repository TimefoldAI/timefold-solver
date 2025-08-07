package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.uni.MapUniToUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniMapUniDataStream<Solution_, A, NewA>
        extends AbstractUniDataStream<Solution_, A> {

    private final UniDataMapper<Solution_, A, NewA> mappingFunction;
    private @Nullable AftBridgeUniDataStream<Solution_, NewA> aftStream;

    public UniMapUniDataStream(DataStreamFactory<Solution_> constraintFactory, AbstractUniDataStream<Solution_, A> parent,
            UniDataMapper<Solution_, A, NewA> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(AftBridgeUniDataStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public boolean guaranteesDistinct() {
        return false;
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = new MapUniToUniNode<>(inputStoreIndex,
                mappingFunction.toFunction(buildHelper.getSessionContext().solutionView()),
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        UniMapUniDataStream<?, ?, ?> that = (UniMapUniDataStream<?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(mappingFunction, that.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunction);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "UniMap()";
    }

}
