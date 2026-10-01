package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import java.util.Objects;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.DataNodeBuildHelper;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.NeighborhoodsGroupNodeConstructor;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.bridge.AftBridgeBiEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class UniGroupBiEnumeratingStream<Solution_, A, NewA, NewB>
        extends AbstractUniEnumeratingStream<Solution_, A> {

    private final NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<NewA, NewB>> nodeConstructor;
    private @Nullable AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream;

    UniGroupBiEnumeratingStream(EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            AbstractUniEnumeratingStream<Solution_, A> parent,
            NeighborhoodsGroupNodeConstructor<Solution_, BiTuple<NewA, NewB>> nodeConstructor) {
        super(enumeratingStreamFactory, parent);
        this.nodeConstructor = Objects.requireNonNull(nodeConstructor);
    }

    void setAftBridge(AftBridgeBiEnumeratingStream<Solution_, NewA, NewB> aftStream) {
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
        var that = (UniGroupBiEnumeratingStream<?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "UniGroupBi()";
    }
}
