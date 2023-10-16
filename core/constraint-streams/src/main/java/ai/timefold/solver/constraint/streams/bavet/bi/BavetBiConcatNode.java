package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public final class BavetBiConcatNode<A, B> extends AbstractConcatNode<BiTuple<A, B>, BiTuple<A, B>, BiTuple<A, B>> {

    BavetBiConcatNode(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return new BiTuple<>(leftTuple.factA, leftTuple.factB, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromRight(BiTuple<A, B> rightTuple) {
        return new BiTuple<>(rightTuple.factA, rightTuple.factB, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void updateOutTupleFromRight(BiTuple<A, B> rightTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
    }

}
