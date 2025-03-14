package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;

final class BavetUniGroupUniConstraintStream<Solution_, A, NewA>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private BavetAftBridgeUniConstraintStream<Solution_, NewA> aftStream;
    private final GroupNodeConstructor<UniTuple<NewA>> nodeConstructor;

    public BavetUniGroupUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent, GroupNodeConstructor<UniTuple<NewA>> nodeConstructor) {
        super(constraintFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(BavetAftBridgeUniConstraintStream<Solution_, NewA> aftStream) {
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
        var that = (BavetUniGroupUniConstraintStream<?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "UniGroup()";
    }

}
