package ai.timefold.solver.constraint.streams.bavet.common.bridge;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.quad.BavetAbstractQuadConstraintStream;
import ai.timefold.solver.core.api.score.Score;

public final class BavetForeBridgeQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> {

    public BavetForeBridgeQuadConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractQuadConstraintStream<Solution_, A, B, C, D> parent) {
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
