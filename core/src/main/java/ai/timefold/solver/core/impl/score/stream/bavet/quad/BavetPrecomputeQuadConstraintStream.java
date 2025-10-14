package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.bi.PrecomputeBiNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeQuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

public class BavetPrecomputeQuadConstraintStream<Solution_, A, B, C, D>
        extends BavetAbstractQuadConstraintStream<Solution_, A, B, C, D>
        implements TupleSource {
    private final BavetAbstractConstraintStream<Solution_> recordingStaticConstraintStream;
    private BavetAftBridgeQuadConstraintStream<Solution_, A, B, C, D> aftStream;

    public BavetPrecomputeQuadConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingStaticConstraintStream = new BavetRecordingQuadConstraintStream<>(constraintFactory,
                staticConstraintStream);
        staticConstraintStream.getChildStreamList().add(recordingStaticConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeQuadConstraintStream<Solution_, A, B, C, D> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        var staticDataBuildHelper = new BavetPrecomputeBuildHelper<BiTuple<A, B>>(recordingStaticConstraintStream);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);

        buildHelper.addNode(new PrecomputeBiNode<>(staticDataBuildHelper.getNodeNetwork(),
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
