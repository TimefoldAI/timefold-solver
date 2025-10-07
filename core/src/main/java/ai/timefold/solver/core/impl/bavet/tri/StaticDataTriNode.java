package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractStaticDataNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StaticDataTriNode<A, B, C> extends AbstractStaticDataNode<TriTuple<A, B, C>> {
    private final int outputStoreSize;

    public StaticDataTriNode(NodeNetwork nodeNetwork,
            RecordingTupleLifecycle<TriTuple<A, B, C>> recordingTupleNode,
            int outputStoreSize,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(nodeNetwork, recordingTupleNode, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, C> remapTuple(TriTuple<A, B, C> tuple) {
        return new TriTuple<>(tuple.factA, tuple.factB, tuple.factC, outputStoreSize);
    }
}
