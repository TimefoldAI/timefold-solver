package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.bi.PrecomputeBiNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.TupleSource;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetPrecomputeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetAftBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;

public class BavetPrecomputeBiConstraintStream<Solution_, A, B> extends BavetAbstractBiConstraintStream<Solution_, A, B>
        implements TupleSource {
    private final BavetAbstractConstraintStream<Solution_> recordingStaticConstraintStream;
    private BavetAftBridgeBiConstraintStream<Solution_, A, B> aftStream;

    public BavetPrecomputeBiConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> staticConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingStaticConstraintStream = new BavetRecordingBiConstraintStream<>(constraintFactory,
                staticConstraintStream);
        staticConstraintStream.getChildStreamList().add(recordingStaticConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeBiConstraintStream<Solution_, A, B> aftStream) {
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
