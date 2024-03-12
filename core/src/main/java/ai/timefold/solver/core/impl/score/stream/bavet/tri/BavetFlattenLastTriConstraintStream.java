package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;

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

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        BavetFlattenLastTriConstraintStream<?, ?, ?, ?, ?> that = (BavetFlattenLastTriConstraintStream<?, ?, ?, ?, ?>) object;
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
