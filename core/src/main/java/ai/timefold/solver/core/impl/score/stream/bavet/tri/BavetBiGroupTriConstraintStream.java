package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;

final class BavetBiGroupTriConstraintStream<Solution_, A, B, C, NewA, NewB>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    private BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream;
    private final GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor;

    public BavetBiGroupTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent,
            GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor) {
        super(constraintFactory, parent);
        this.nodeConstructor = nodeConstructor;
    }

    public void setAftBridge(BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream) {
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
        var that = (BavetBiGroupTriConstraintStream<?, ?, ?, ?, ?, ?>) object;
        return Objects.equals(parent, that.parent) && Objects.equals(nodeConstructor, that.nodeConstructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, nodeConstructor);
    }

    @Override
    public String toString() {
        return "BiGroup()";
    }

}
