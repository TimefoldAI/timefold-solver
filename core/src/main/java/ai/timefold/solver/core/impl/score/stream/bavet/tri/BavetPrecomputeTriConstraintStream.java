package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

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
    private final BavetAbstractConstraintStream<Solution_> recordingPrecomputedConstraintStream;
    private BavetAftBridgeTriConstraintStream<Solution_, A, B, C> aftStream;

    public BavetPrecomputeTriConstraintStream(
            BavetConstraintFactory<Solution_> constraintFactory,
            BavetAbstractConstraintStream<Solution_> precomputedConstraintStream) {
        super(constraintFactory, RetrievalSemantics.STANDARD);
        this.recordingPrecomputedConstraintStream = new BavetRecordingTriConstraintStream<>(constraintFactory,
                precomputedConstraintStream);
        precomputedConstraintStream.getChildStreamList().add(recordingPrecomputedConstraintStream);
    }

    public void setAftBridge(BavetAftBridgeTriConstraintStream<Solution_, A, B, C> aftStream) {
        this.aftStream = aftStream;
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        Supplier<BavetPrecomputeBuildHelper<TriTuple<A, B, C>>> precomputeBuildHelperSupplier =
                () -> new BavetPrecomputeBuildHelper<>(recordingPrecomputedConstraintStream);
        var outputStoreSize = buildHelper.extractTupleStoreSize(aftStream);

        buildHelper.addNode(new PrecomputeTriNode<>(precomputeBuildHelperSupplier,
                outputStoreSize,
                buildHelper.getAggregatedTupleLifecycle(aftStream.getChildStreamList()),
                precomputeBuildHelperSupplier.get().getSourceClasses()),
                this);
    }

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        constraintStreamSet.add(this);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public int hashCode() {
        return Objects.hash(recordingPrecomputedConstraintStream);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BavetPrecomputeTriConstraintStream<?, ?, ?, ?> other) {
            return recordingPrecomputedConstraintStream.equals(other.recordingPrecomputedConstraintStream);
        } else {
            return false;
        }
    }
}
