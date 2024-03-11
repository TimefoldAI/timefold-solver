package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;

final class BavetQuadGroupQuadConstraintStream<Solution_, A, B, C, D, NewA, NewB, NewC, NewD>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    private final GroupNodeConstructor<QuadTuple<NewA, NewB, NewC, NewD>> nodeConstructor;
    private BavetAftBridgeQuadConstraintStream<Solution_, NewA, NewB, NewC, NewD> aftStream;

    public BavetQuadGroupQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent,
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

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (BavetQuadGroupQuadConstraintStream<?, ?, ?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "QuadGroup()";
    }

}
