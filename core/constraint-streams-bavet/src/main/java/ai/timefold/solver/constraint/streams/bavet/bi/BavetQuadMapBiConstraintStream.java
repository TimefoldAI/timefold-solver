package ai.timefold.solver.constraint.streams.bavet.bi;

import java.util.function.BiFunction;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.api.score.Score;

final class BavetQuadMapBiConstraintStream<Solution_, A, B, NewA, NewB, NewC, NewD>
        extends BavetAbstractBiConstraintStream<Solution_, A, B> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private final BiFunction<A, B, NewC> mappingFunctionC;
    private final BiFunction<A, B, NewD> mappingFunctionD;
    private final boolean guaranteesDistinct;

    private BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream;

    public BavetQuadMapBiConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractBiConstraintStream<Solution_, A, B> parent, BiFunction<A, B, NewA> mappingFunctionA,
            BiFunction<A, B, NewB> mappingFunctionB, BiFunction<A, B, NewC> mappingFunctionC,
            BiFunction<A, B, NewD> mappingFunctionD, boolean isExpand) {
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
                new MapBiToQuadNode<>(inputStoreIndex, mappingFunctionA, mappingFunctionB, mappingFunctionC, mappingFunctionD,
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
        return "QuadMap()";
    }

}
