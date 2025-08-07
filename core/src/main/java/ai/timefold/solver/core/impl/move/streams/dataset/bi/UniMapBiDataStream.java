package ai.timefold.solver.core.impl.move.streams.dataset.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.bi.MapBiToUniNode;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeUniDataStream;
import ai.timefold.solver.core.impl.move.streams.dataset.uni.AbstractUniDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniMapBiDataStream<Solution_, A, B, NewA, NewB>
        extends AbstractUniDataStream<Solution_, NewA> {

    private final BiDataMapper<Solution_, A, B, NewA> mappingFunction;
    private @Nullable AftBridgeUniDataStream<Solution_, NewA> aftStream;

    public UniMapBiDataStream(DataStreamFactory<Solution_> constraintFactory, AbstractBiDataStream<Solution_, A, B> parent,
            BiDataMapper<Solution_, A, B, NewA> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(AftBridgeUniDataStream<Solution_, NewA> aftStream) {
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
        UniMapBiDataStream<?, ?, ?, ?, ?> that = (UniMapBiDataStream<?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(mappingFunction, that.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunction);
    }

    @Override
    public String toString() {
        return "BiMap()";
    }

}
