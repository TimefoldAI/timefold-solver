package ai.timefold.solver.core.impl.bavet.uni;

import ai.timefold.solver.core.impl.bavet.NodeNetwork;
import ai.timefold.solver.core.impl.bavet.common.AbstractStaticDataNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.RecordingTupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StaticDataUniNode<A> extends AbstractStaticDataNode<UniTuple<A>> {
    private final int outputStoreSize;

    public StaticDataUniNode(NodeNetwork nodeNetwork,
            RecordingTupleLifecycle<UniTuple<A>> recordingTupleNode,
            int outputStoreSize,
            TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
            Class<?>[] sourceClasses) {
        super(nodeNetwork, recordingTupleNode, nextNodesTupleLifecycle, sourceClasses);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected UniTuple<A> remapTuple(UniTuple<A> tuple) {
        return new UniTuple<>(tuple.factA, outputStoreSize);
    }
}
