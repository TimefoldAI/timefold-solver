package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.NeighborhoodsGroupNodeConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeUniEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class BiGroupUniEnumeratingStream<Solution_, A, B, NewA>
        extends AbstractBiEnumeratingStream<Solution_, A, B> {

    private final NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<NewA>> nodeConstructor;
    private @Nullable AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream;

    BiGroupUniEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractBiEnumeratingStream<Solution_, A, B> parent,
            NeighborhoodsGroupNodeConstructor<Solution_, UniTuple<NewA>> nodeConstructor) {
        super(enumeratingStreamFactory, parent);
        this.nodeConstructor = Objects.requireNonNull(nodeConstructor);
    }

    void setAftBridge(AftBridgeUniEnumeratingStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public boolean guaranteesDistinct() {
        return true;
    }

    @Override
    public void buildNode(DataNodeBuildHelper<Solution_> buildHelper) {
        var view = buildHelper.getSessionContext().solutionView();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream,
                aftStream.getChildStreamList(), this, enumeratingStreamFactory.getEnvironmentMode(), view);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (BiGroupUniEnumeratingStream<?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "BiGroupUni()";
    }
}
