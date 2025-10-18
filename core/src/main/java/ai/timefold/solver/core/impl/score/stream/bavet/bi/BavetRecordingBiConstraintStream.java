package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;

public class BavetRecordingBiConstraintStream<Solution_, A, B> extends BavetAbstractBiConstraintStream<Solution_, A, B> {
    protected BavetRecordingBiConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        assertEmptyChildStreamList();
        buildHelper.putInsertUpdateRetract(this, TupleLifecycle.recording());
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetRecordingBiConstraintStream<?, ?, ?> other) {
            return parent.equals(other.parent);
        } else {
            return false;
        }
    }
}
