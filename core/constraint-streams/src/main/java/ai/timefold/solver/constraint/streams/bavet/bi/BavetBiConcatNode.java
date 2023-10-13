package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public final class BavetBiConcatNode<A, B> extends AbstractConcatNode<BiTuple<A, B>> {

    BavetBiConcatNode(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTuple(BiTuple<A, B> inTuple) {
        return new BiTuple<>(inTuple.factA, inTuple.factB, outputStoreSize);
    }

    @Override
    protected void updateOutTuple(BiTuple<A, B> inTuple, BiTuple<A, B> outTuple) {
        outTuple.updateIfDifferent(inTuple.factA, inTuple.factB);
    }
}
