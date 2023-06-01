package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;

final class BavetUniMapBiConstraintStream<Solution_, A, B, NewA>
        extends BavetAbstractBiConstraintStream<Solution_, A, B> {

    private final BiFunction<A, B, NewA> mappingFunction;
    private BavetAftBridgeUniConstraintStream<Solution_, NewA> aftStream;

    public BavetUniMapBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, BiFunction<A, B, NewA> mappingFunction) {
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
        var node = new MapBiToUniNode<>(inputStoreIndex, mappingFunction,
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()), outputStoreSize);
        buildHelper.addNode(node, this);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    // TODO

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "UniMap()";
    }

}
