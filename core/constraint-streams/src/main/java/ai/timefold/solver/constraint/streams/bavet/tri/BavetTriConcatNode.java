package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public final class BavetTriConcatNode<A, B, C> extends AbstractConcatNode<TriTuple<A, B, C>> {

    BavetTriConcatNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTuple(TriTuple<A, B, C> inTuple) {
        return new TriTuple<>(inTuple.factA, inTuple.factB, inTuple.factC, outputStoreSize);
    }

    @Override
    protected void updateOutTuple(TriTuple<A, B, C> inTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = inTuple.factA;
        outTuple.factB = inTuple.factB;
        outTuple.factC = inTuple.factC;
    }
}
