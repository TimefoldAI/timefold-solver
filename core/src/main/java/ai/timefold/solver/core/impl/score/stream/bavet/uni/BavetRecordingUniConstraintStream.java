package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleRecorder;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;

public class BavetRecordingUniConstraintStream<Solution_, A> extends BavetAbstractUniConstraintStream<Solution_, A> {
    protected BavetRecordingUniConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> parent) {
        super(constraintFactory, parent);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        assertEmptyChildStreamList();
        buildHelper.addNode(new RecordingTupleNode<>(new TupleRecorder<>()), this);
    }
}
