package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;

final class BavetBiMapQuadConstraintStream<Solution_, A, B, C, D, NewA, NewB>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;
    private BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream;

    public BavetBiMapQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
    }

    public void setAftBridge(BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream) {
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
        var node = new MapQuadToBiNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB,
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
        BavetBiMapQuadConstraintStream<?, ?, ?, ?, ?, ?, ?> that = (BavetBiMapQuadConstraintStream<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(mappingFunctionA, that.mappingFunctionA) && Objects.equals(
                mappingFunctionB,
                that.mappingFunctionB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunctionA, mappingFunctionB);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "BiMap()";
    }

}
