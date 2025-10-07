package ai.timefold.solver.core.impl.bavet.quad;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractStaticDataNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StaticDataQuadNode<A, B, C, D> extends AbstractStaticDataNode<QuadTuple<A, B, C, D>> {
    private final int outputStoreSize;

    public StaticDataQuadNode(NodeNetwork nodeNetwork,
            RecordingTupleLifecycle<QuadTuple<A, B, C, D>> recordingTupleNode,
            int outputStoreSize,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(nodeNetwork, recordingTupleNode, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, D> remapTuple(QuadTuple<A, B, C, D> tuple) {
        return new QuadTuple<>(tuple.factA, tuple.factB, tuple.factC, tuple.factD,
                outputStoreSize);
    }
}
