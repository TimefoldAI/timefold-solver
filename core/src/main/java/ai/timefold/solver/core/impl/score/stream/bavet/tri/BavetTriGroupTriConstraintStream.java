package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;

final class BavetTriGroupTriConstraintStream<Solution_, A, B, C, NewA, NewB, NewC>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private final GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor;
    private BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream;

    public BavetTriGroupTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            GroupNodeConstructor<TriTuple<NewA, NewB, NewC>> nodeConstructor) {
        super(constraintFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, NewA, NewB, NewC> aftStream) {
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
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this,
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
        var that = (BavetTriGroupTriConstraintStream<?, ?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "TriGroup()";
    }

}
