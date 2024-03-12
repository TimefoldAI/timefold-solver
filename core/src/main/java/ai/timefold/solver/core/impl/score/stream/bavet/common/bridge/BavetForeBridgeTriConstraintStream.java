package ai.timefold.solver.core.impl.score.stream.bavet.common.bridge;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.NodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.tri.BavetAbstractTriConstraintStream;

public final class BavetForeBridgeTriConstraintStream<Solution_, A, B, C>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C> {

    public BavetForeBridgeTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractTriConstraintStream<Solution_, A, B, C> parent) {
        super(constraintFactory, parent);
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        // Do nothing. The child stream builds everything.
    }

    @Override
    public String toString() {
        return "Generic bridge";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

}
