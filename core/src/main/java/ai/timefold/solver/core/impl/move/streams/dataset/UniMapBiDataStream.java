package ai.timefold.solver.core.impl.move.streams.dataset;

import ai.timefold.solver.core.impl.bavet.uni.MapUniToBiNode;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeBiDataStream;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
final class UniMapBiDataStream<Solution_, A, NewA, NewB>
        extends AbstractBiDataStream<Solution_, NewA, NewB> {

    private final UniDataMapper<Solution_, A, NewA> mappingFunctionA;
    private final UniDataMapper<Solution_, A, NewB> mappingFunctionB;
    private @Nullable AftBridgeBiDataStream<Solution_, NewA, NewB> aftStream;

    public UniMapBiDataStream(DataStreamFactory<Solution_> constraintFactory, AbstractUniDataStream<Solution_, A> parent, UniDataMapper<Solution_, A, NewA> mappingFunctionA, UniDataMapper<Solution_, A, NewB> mappingFunctionB) {
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
        UniMapBiDataStream<?, ?, ?, ?> that = (UniMapBiDataStream<?, ?, ?, ?>) object;
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
