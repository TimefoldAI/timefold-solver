package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;

final class BavetTriMapUniConstraintStream<Solution_, A, NewA, NewB, NewC>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;
    private final Function<A, NewC> mappingFunctionC;
    private final boolean guaranteesDistinct;

    private BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream;

    public BavetTriMapUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, Function<A, NewA> mappingFunctionA,
            Function<A, NewB> mappingFunctionB, Function<A, NewC> mappingFunctionC, boolean isExpand) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
        this.mappingFunctionC = mappingFunctionC;
        this.guaranteesDistinct = isExpand && parent.guaranteesDistinct();
    }

    @Override
    public boolean guaranteesDistinct() {
        return guaranteesDistinct;
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream) {
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
        var node = new MapUniToTriNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB, mappingFunctionC,
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
        BavetTriMapUniConstraintStream<?, ?, ?, ?, ?> that = (BavetTriMapUniConstraintStream<?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && guaranteesDistinct == that.guaranteesDistinct && Objects.equals(
                mappingFunctionA,
                that.mappingFunctionA) && Objects.equals(mappingFunctionB,
                        that.mappingFunctionB)
                && Objects.equals(mappingFunctionC, that.mappingFunctionC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunctionA, mappingFunctionB, mappingFunctionC, guaranteesDistinct);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "TriMap()";
    }

}
