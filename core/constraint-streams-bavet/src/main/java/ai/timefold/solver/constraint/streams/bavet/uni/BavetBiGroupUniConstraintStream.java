package ai.timefold.solver.constraint.streams.bavet.uni;

import java.util.List;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.GroupNodeConstructor;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;

final class BavetBiGroupUniConstraintStream<Solution_, A, NewA, NewB>
        extends BavetAbstractUniConstraintStream<Solution_, A> {

    private final GroupNodeConstructor<BiTuple<NewA, NewB>> nodeConstructor;
    private BavetAftBridgeBiConstraintStream<Solution_, NewA, NewB> aftStream;

    public BavetBiGroupUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractUniConstraintStream<Solution_, A> parent,
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
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        List<? extends ConstraintStream> aftStreamChildList = aftStream.getChildStreamList();
        nodeConstructor.build(buildHelper, parent.getTupleSource(), aftStream, aftStreamChildList, this, childStreamList,
                constraintFactory.getEnvironmentMode());
    }

    @Override
    public String toString() {
        return "BiGroup()";
    }

}
