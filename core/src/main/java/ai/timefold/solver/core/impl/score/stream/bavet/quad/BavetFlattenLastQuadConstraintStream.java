package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;

final class BavetFlattenLastQuadConstraintStream<Solution_, A, B, C, D, NewD>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    private final Function<D, Iterable<NewD>> mappingFunction;
    private BavetAftBridgeQuadConstraintStream<Solution_, A, B, C, NewD> flattenLastStream;

    public BavetFlattenLastQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
            Function<D, Iterable<NewD>> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(BavetAftBridgeQuadConstraintStream<Solution_, A, B, C, NewD> flattenLastStream) {
        this.flattenLastStream = flattenLastStream;
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
        int outputStoreSize = buildHelper.extractTupleStoreSize(flattenLastStream);
        var node = new FlattenLastQuadNode<>(inputStoreIndex, mappingFunction,
                buildHelper.getAggregatedTupleLifecycle(flattenLastStream.getChildStreamList()),
                outputStoreSize);
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
        BavetFlattenLastQuadConstraintStream<?, ?, ?, ?, ?, ?> that =
                (BavetFlattenLastQuadConstraintStream<?, ?, ?, ?, ?, ?>) object;
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
        return "FlattenLast()";
    }

}
