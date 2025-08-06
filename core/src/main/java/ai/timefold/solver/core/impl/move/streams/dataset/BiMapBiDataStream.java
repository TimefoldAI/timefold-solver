package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.bi.MapBiToBiNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeBiDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
final class BiMapBiDataStream<Solution_, A, B, NewA, NewB>
        extends AbstractBiDataStream<Solution_, NewA, NewB> {

    private final BiDataMapper<Solution_, A, B, NewA> mappingFunctionA;
    private final BiDataMapper<Solution_, A, B, NewB> mappingFunctionB;
    private @Nullable AftBridgeBiDataStream<Solution_, NewA, NewB> aftStream;

    public BiMapBiDataStream(DataStreamFactory<Solution_> constraintFactory, AbstractBiDataStream<Solution_, A, B> parent,
            BiDataMapper<Solution_, A, B, NewA> mappingFunctionA, BiDataMapper<Solution_, A, B, NewB> mappingFunctionB) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
    }

    public void setAftBridge(AftBridgeBiDataStream<Solution_, NewA, NewB> aftStream) {
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
        var node = new MapBiToBiNode<>(inputStoreIndex,
                mappingFunctionA.toBiFunction(buildHelper.getSessionContext().solutionView()),
                mappingFunctionB.toBiFunction(buildHelper.getSessionContext().solutionView()),
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BiMapBiDataStream<?, ?, ?, ?, ?> that = (BiMapBiDataStream<?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(mappingFunctionA, that.mappingFunctionA) &&
                Objects.equals(mappingFunctionB, that.mappingFunctionB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunctionA, mappingFunctionB);
    }

    @Override
    public String toString() {
        return "BiMap()";
    }

}
