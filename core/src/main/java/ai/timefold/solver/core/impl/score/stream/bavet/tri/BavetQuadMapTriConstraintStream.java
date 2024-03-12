package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;

final class BavetQuadMapTriConstraintStream<Solution_, A, B, C, NewA, NewB, NewC, NewD>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;
    private final TriFunction<A, B, C, NewC> mappingFunctionC;
    private final TriFunction<A, B, C, NewD> mappingFunctionD;
    private final boolean guaranteesDistinct;
    private BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream;

    public BavetQuadMapTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent, TriFunction<A, B, C, NewA> mappingFunctionA,
            TriFunction<A, B, C, NewB> mappingFunctionB, TriFunction<A, B, C, NewC> mappingFunctionC,
            TriFunction<A, B, C, NewD> mappingFunctionD, boolean isExpand) {
        super(constraintFactory, parent);
        this.mappingFunctionA = mappingFunctionA;
        this.mappingFunctionB = mappingFunctionB;
        this.mappingFunctionC = mappingFunctionC;
        this.mappingFunctionD = mappingFunctionD;
        this.guaranteesDistinct = isExpand && parent.guaranteesDistinct();
    }

    @Override
    public boolean guaranteesDistinct() {
        return guaranteesDistinct;
    }

    public void setAftBridge(BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream) {
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
        var node =
                new MapTriToQuadNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB, mappingFunctionC, mappingFunctionD,
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
        BavetQuadMapTriConstraintStream<?, ?, ?, ?, ?, ?, ?, ?> that =
                (BavetQuadMapTriConstraintStream<?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && guaranteesDistinct == that.guaranteesDistinct && Objects.equals(
                mappingFunctionA,
                that.mappingFunctionA) && Objects.equals(mappingFunctionB,
                        that.mappingFunctionB)
                && Objects.equals(mappingFunctionC,
                        that.mappingFunctionC)
                && Objects.equals(mappingFunctionD, that.mappingFunctionD);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, mappingFunctionA, mappingFunctionB, mappingFunctionC, mappingFunctionD, guaranteesDistinct);
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String toString() {
        return "QuadMap()";
    }

}
