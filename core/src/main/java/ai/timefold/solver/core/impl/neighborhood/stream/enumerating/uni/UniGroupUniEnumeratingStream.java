package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniGroupUniEnumeratingStream<Solution_, A, NewA>
        extends AbstractUniEnumeratingStream<Solution_, A> {

    private final GroupNodeConstructor<UniTuple<NewA>> nodeConstructor;
    private @Nullable AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream;

    public UniGroupUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent,
            GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        super(enumeratingStreamFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this,
                enumeratingStreamFactory.getEnvironmentMode());
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
        var that = (UniGroupUniEnumeratingStream<?, ?, ?>) object;
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
