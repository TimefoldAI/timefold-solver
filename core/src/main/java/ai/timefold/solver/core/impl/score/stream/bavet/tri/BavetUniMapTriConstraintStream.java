package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;

final class BavetUniMapTriConstraintStream<Solution_, A, B, C, NewA>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final TriFunction<A, B, C, NewA> mappingFunction;
    private BavetAftBridgeUniConstraintStream<Solution_, NewA> aftStream;

    public BavetUniMapTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, TriFunction<A, B, C, NewA> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(BavetAftBridgeUniConstraintStream<Solution_, NewA> aftStream) {
        this.aftStream = aftStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public boolean guaranteesDistinct() {
        return false;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        assertEmptyChildStreamList();
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = new MapTriToUniNode<>(inputStoreIndex, mappingFunction,
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
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
        BavetUniMapTriConstraintStream<?, ?, ?, ?, ?> that = (BavetUniMapTriConstraintStream<?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(mappingFunction, that.mappingFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunction);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "UniMap()";
    }

}
