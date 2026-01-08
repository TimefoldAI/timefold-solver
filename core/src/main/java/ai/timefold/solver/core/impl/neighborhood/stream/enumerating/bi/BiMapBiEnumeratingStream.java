package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.bi.MapBiToBiNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BiMapBiEnumeratingStream<Solution_, A, B, NewA, NewB>
        extends AbstractBiEnumeratingStream<Solution_, NewA, NewB> {

    private final BiNeighborhoodsMapper<Solution_, A, B, NewA> mappingFunctionA;
    private final BiNeighborhoodsMapper<Solution_, A, B, NewB> mappingFunctionB;
    private @Nullable AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream;

    public BiMapBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent,
            BiNeighborhoodsMapper<Solution_, A, B, NewA> mappingFunctionA,
            BiNeighborhoodsMapper<Solution_, A, B, NewB> mappingFunctionB) {
        super(enumeratingStreamFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
    }

    public void setAftBridge(AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream) {
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
        BiMapBiEnumeratingStream<?, ?, ?, ?, ?> that = (BiMapBiEnumeratingStream<?, ?, ?, ?, ?>) object;
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
