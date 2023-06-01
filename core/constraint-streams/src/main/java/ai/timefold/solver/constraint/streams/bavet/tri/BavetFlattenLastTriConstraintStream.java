package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.core.api.score.Score;

final class BavetFlattenLastTriConstraintStream<Solution_, A, B, C, NewC>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final Function<C, Iterable<NewC>> mappingFunction;
    private BavetAftBridgeTriConstraintStream<Solution_, A, B, NewC> flattenLastStream;

    public BavetFlattenLastTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            Function<C, Iterable<NewC>> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, A, B, NewC> flattenLastStream) {
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
        var node = new FlattenLastTriNode<>(inputStoreIndex, mappingFunction,
                buildHelper.getAggregatedTupleLifecycle(flattenLastStream.getChildStreamList()),
                outputStoreSize);
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
        return "FlattenLast()";
    }

}
