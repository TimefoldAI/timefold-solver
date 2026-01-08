package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.uni.MapUniToBiNode;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.AbstractBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.function.UniNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BiMapUniEnumeratingStream<Solution_, A, NewA, NewB>
        extends AbstractBiEnumeratingStream<Solution_, NewA, NewB> {

    private final UniNeighborhoodsMapper<Solution_, A, NewA> mappingFunctionA;
    private final UniNeighborhoodsMapper<Solution_, A, NewB> mappingFunctionB;
    private @Nullable AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream;

    public BiMapUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent,
            UniNeighborhoodsMapper<Solution_, A, NewA> mappingFunctionA,
            UniNeighborhoodsMapper<Solution_, A, NewB> mappingFunctionB) {
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
        var node = new MapUniToBiNode<>(inputStoreIndex,
                mappingFunctionA.toFunction(buildHelper.getSessionContext().solutionView()),
                mappingFunctionB.toFunction(buildHelper.getSessionContext().solutionView()),
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BiMapUniEnumeratingStream<?, ?, ?, ?> that = (BiMapUniEnumeratingStream<?, ?, ?, ?>) object;
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
        return "UniMap()";
    }

}
