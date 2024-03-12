package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;

final class BavetTriMapTriConstraintStream<Solution_, A, B, C, NewA, NewB, NewC>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;
    private final TriFunction<A, B, C, NewC> mappingFunctionC;
    private BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream;

    public BavetTriMapTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, TriFunction<A, B, C, NewA> mappingFunctionA,
            TriFunction<A, B, C, NewB> mappingFunctionB, TriFunction<A, B, C, NewC> mappingFunctionC) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
        this.mappingFunctionC = mappingFunctionC;
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream) {
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
        var node = new MapTriToTriNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB, mappingFunctionC,
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
        BavetTriMapTriConstraintStream<?, ?, ?, ?, ?, ?, ?> that = (BavetTriMapTriConstraintStream<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(mappingFunctionA, that.mappingFunctionA) && Objects.equals(
                mappingFunctionB,
                that.mappingFunctionB) && Objects.equals(mappingFunctionC, that.mappingFunctionC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunctionA, mappingFunctionB, mappingFunctionC);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "TriMap()";
    }

}
