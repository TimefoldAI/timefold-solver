package ai.timefold.solver.constraint.streams.bavet.tri;

import java.util.List;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;

final class BavetQuadGroupTriConstraintStream<Solution_, A, B, C, NewA, NewB, NewC, NewD>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor;
    private BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream;

    public BavetQuadGroupTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor) {
        super(constraintFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public boolean guaranteesDistinct() {
        return true;
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        List<? extends ConstraintStream> aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this, childStreamList,
                constraintFactory.getEnvironmentMode());
    }

    @Override
    public String toString() {
        return "QuadGroup()";
    }

}
