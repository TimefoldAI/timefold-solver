package ai.timefold.solver.constraint.streams.bavet.common.bridge;

import java.util.Objects;

import ai.timefold.solver.constraint.streams.bavet.BavetConstraintFactory;
import ai.timefold.solver.constraint.streams.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.constraint.streams.bavet.common.NodeBuildHelper;
import ai.timefold.solver.constraint.streams.bavet.common.TupleSource;
import ai.timefold.solver.constraint.streams.bavet.uni.BavetAbstractUniConstraintStream;
import ai.timefold.solver.core.api.score.Score;

public final class BavetAftBridgeUniConstraintStream<Solution_, A>
        extends BavetAbstractUniConstraintStream<Solution_, A>
        implements TupleSource {

    public BavetAftBridgeUniConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(NodeBuildHelper<Score_> buildHelper) {
        // Do nothing. The parent stream builds everything.
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BavetAftBridgeUniConstraintStream<?, ?> that = (BavetAftBridgeUniConstraintStream<?, ?>) o;
        return Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public String toString() {
        return "Bridge from " + parent + " with " + childStreamList.size() + " children";
    }

}
