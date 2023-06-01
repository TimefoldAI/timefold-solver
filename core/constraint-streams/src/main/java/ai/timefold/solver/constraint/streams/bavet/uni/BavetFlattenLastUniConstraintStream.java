package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.function.Function;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;

final class BavetFlattenLastUniConstraintStream<Solution_, A, NewA>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final Function<A, Iterable<NewA>> mappingFunction;
    private BavetAftBridgeUniConstraintStream<Solution_, NewA> flattenLastStream;

    public BavetFlattenLastUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
            Function<A, Iterable<NewA>> mappingFunction) {
        super(constraintFactory, parent);
        this.mappingFunction = mappingFunction;
    }

    public void setAftBridge(BavetAftBridgeUniConstraintStream<Solution_, NewA> flattenLastStream) {
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
        var node = new FlattenLastUniNode<>(inputStoreIndex, mappingFunction,
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
