package ai.timefold.solver.core.impl.move.streams.dataset.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.DataStreamFactory;
import ai.timefold.solver.core.impl.move.streams.dataset.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.move.streams.dataset.common.bridge.AftBridgeUniDataStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniGroupUniDataStream<Solution_, A, NewA>
        extends AbstractUniDataStream<Solution_, A> {

    private final GroupNodeConstructor<UniTuple<NewA>> nodeConstructor;
    private @Nullable AftBridgeUniDataStream<Solution_, NewA> aftStream;

    public UniGroupUniDataStream(DataStreamFactory<Solution_> dataStreamFactory, AbstractUniDataStream<Solution_, A> parent,
            GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        super(dataStreamFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(AftBridgeUniDataStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this,
                dataStreamFactory.getEnvironmentMode());
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
        var that = (UniGroupUniDataStream<?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "UniGroup()";
    }

}
