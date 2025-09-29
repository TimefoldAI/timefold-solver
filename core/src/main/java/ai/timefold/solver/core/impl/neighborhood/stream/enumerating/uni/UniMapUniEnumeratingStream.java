package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.uni.MapUniToUniNode;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.UniEnumeratingMapper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniMapUniEnumeratingStream<Solution_, A, NewA>
        extends AbstractUniEnumeratingStream<Solution_, A> {

    private final UniEnumeratingMapper<Solution_, A, NewA> mappingFunction;
    private @Nullable AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream;

    public UniMapUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent,
            UniEnumeratingMapper<Solution_, A, NewA> mappingFunction) {
        super(enumeratingStreamFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream) {
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
        UniMapUniEnumeratingStream<?, ?, ?> that = (UniMapUniEnumeratingStream<?, ?, ?>) object;
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
