package ai.timefold.solver.core.impl.move.streams.dataset.bi;

import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeBiDataStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
final class BiGroupBiDataStream<Solution_, A, B, NewA, NewB>
        extends AbstractBiDataStream<Solution_, A, B> {

    private final GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor;
    private @Nullable AftBridgeBiDataStream<Solution_, NewA, NewB> aftStream;

    public BiGroupBiDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractBiDataStream<Solution_, A, B> parent,
            GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        super(dataStreamFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(AftBridgeBiDataStream<Solution_, NewA, NewB> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this,
                dataStreamFactory.getEnvironmentMode());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (BiGroupBiDataStream<?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "BiGroup()";
    }

}
