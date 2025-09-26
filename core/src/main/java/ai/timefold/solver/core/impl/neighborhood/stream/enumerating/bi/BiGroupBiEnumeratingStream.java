package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BiGroupBiEnumeratingStream<Solution_, A, B, NewA, NewB>
        extends AbstractBiEnumeratingStream<Solution_, A, B> {

    private final GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor;
    private @Nullable AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream;

    public BiGroupBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent,
            GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        super(enumeratingStreamFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this,
                enumeratingStreamFactory.getEnvironmentMode());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (BiGroupBiEnumeratingStream<?, ?, ?, ?, ?>) object;
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
