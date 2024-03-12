package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;

final class BavetBiMapBiConstraintStream<Solution_, A, B, NewA, NewB>
        extends BavetAbstractBiConstraintStream<Solution_, A, B> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream;

    public BavetBiMapBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, BiFunction<A, B, NewA> mappingFunctionA,
            BiFunction<A, B, NewB> mappingFunctionB) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
    }

    @Override
    public boolean guaranteesDistinct() {
        return false;
    }

    public void setAftBridge(BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream) {
        this.aftStream = aftStream;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        assertEmptyChildStreamList();
        int inputStoreIndex = buildHelper.reserveTupleStoreIndex(parent.getTupleSource());
        int outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);
        var node = new MapBiToBiNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB,
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
        BavetBiMapBiConstraintStream<?, ?, ?, ?, ?> that = (BavetBiMapBiConstraintStream<?, ?, ?, ?, ?>) object;
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
