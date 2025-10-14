package ai.timefold.solver.core.impl.score.stream.bavet.uni;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.bavet.uni.PrecomputeUniNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeUniConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

public class BavetPrecomputeUniConstraintStream<Solution_, A> extends BavetAbstractUniConstraintStream<Solution_, A>
        implements TupleSource {
    private final BavetAbstractConstraintStream<Solution_> recordingStaticConstraintStream;
    private BavetAftBridgeUniConstraintStream<Solution_, A> aftStream;

    public BavetPrecomputeUniConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingStaticConstraintStream = new BavetRecordingUniConstraintStream<>(constraintFactory,
                staticConstraintStream);
        staticConstraintStream.getChildStreamList().add(recordingStaticConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeUniConstraintStream<Solution_, A> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var staticDataBuildHelper = new BavetPrecomputeBuildHelper<UniTuple<A>>(recordingStaticConstraintStream);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);

        buildHelper.addNode(new PrecomputeUniNode<>(staticDataBuildHelper.getNodeNetwork(),
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
