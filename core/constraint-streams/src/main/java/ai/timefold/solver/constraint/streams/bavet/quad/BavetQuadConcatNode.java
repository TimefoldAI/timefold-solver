package ai.timefold.solver.constraint.streams.bavet.quad;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public final class BavetQuadConcatNode<A, B, C, D> extends AbstractConcatNode<QuadTuple<A, B, C, D>> {

    BavetQuadConcatNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList, int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTuple(QuadTuple<A, B, C, D> inTuple) {
        return new QuadTuple<>(inTuple.factA, inTuple.factB, inTuple.factC, inTuple.factD, outputStoreSize);
    }

    @Override
    protected void updateOutTuple(QuadTuple<A, B, C, D> inTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = inTuple.factA;
        outTuple.factB = inTuple.factB;
        outTuple.factC = inTuple.factC;
        outTuple.factD = inTuple.factD;
    }
}
