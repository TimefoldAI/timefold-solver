package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.bi.MapBiToUniNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.function.BiNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniMapBiEnumeratingStream<Solution_, A, B, NewA>
        extends AbstractUniEnumeratingStream<Solution_, NewA> {

    private final BiNeighborhoodsMapper<Solution_, A, B, NewA> mappingFunction;
    private @Nullable AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream;

    public UniMapBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent,
            BiNeighborhoodsMapper<Solution_, A, B, NewA> mappingFunction) {
        super(enumeratingStreamFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public boolean guaranteesDistinct() {
        return false;
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        assertEmptyChildStreamList();
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = new MapBiToUniNode<>(inputStoreIndex,
                mappingFunction.toBiFunction(buildHelper.getSessionContext().solutionView()),
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        UniMapBiEnumeratingStream<?, ?, ?, ?> that = (UniMapBiEnumeratingStream<?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(mappingFunction, that.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunction);
    }

    @Override
    public String toString() {
        return "UniMap()";
    }

}
