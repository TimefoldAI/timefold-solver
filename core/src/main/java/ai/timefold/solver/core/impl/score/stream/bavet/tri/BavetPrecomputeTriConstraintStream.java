package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.tri.PrecomputeTriNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeTriConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

public class BavetPrecomputeTriConstraintStream<Solution_, A, B, C> extends BavetAbstractTriConstraintStream<Solution_, A, B, C>
        implements TupleSource {
    private final BavetAbstractConstraintStream<Solution_> recordingStaticConstraintStream;
    private BavetAftBridgeTriConstraintStream<Solution_, A, B, C> aftStream;

    public BavetPrecomputeTriConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingStaticConstraintStream = new BavetRecordingTriConstraintStream<>(constraintFactory,
                staticConstraintStream);
        staticConstraintStream.getChildStreamList().add(recordingStaticConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, A, B, C> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var staticDataBuildHelper = new BavetPrecomputeBuildHelper<TriTuple<A, B, C>>(recordingStaticConstraintStream);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);

        buildHelper.addNode(new PrecomputeTriNode<>(staticDataBuildHelper.getNodeNetwork(),
                staticDataBuildHelper.getRecordingTupleLifecycle(),
                outputStoreSize,
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()),
                staticDataBuildHelper.getSourceClasses()),
                this);
    }

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        constraintStreamSet.add(this);
    }
}
