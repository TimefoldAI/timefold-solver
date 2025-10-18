package ai.timefold.solver.core.impl.bavet.bi;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractPrecomputeNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PrecomputeBiNode<A, B> extends AbstractPrecomputeNode<BiTuple<A, B>> {
    private final int outputStoreSize;

    public PrecomputeBiNode(NodeNetwork nodeNetwork,
            RecordingTupleLifecycle<BiTuple<A, B>> recordingTupleNode,
            int outputStoreSize,
            TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(nodeNetwork, recordingTupleNode, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected BiTuple<A, B> remapTuple(BiTuple<A, B> tuple) {
        return new BiTuple<>(tuple.factA, tuple.factB, outputStoreSize);
    }
}
