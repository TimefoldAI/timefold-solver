package ai.timefold.solver.constraint.streams.bavet.bi;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

final class BavetUniBiConcatNode<A, B>
        extends AbstractConcatNode<UniTuple<A>, BiTuple<A, B>, BiTuple<A, B>> {

    BavetUniBiConcatNode(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromLeft(UniTuple<A> leftTuple) {
        return new BiTuple<>(leftTuple.factA, null, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromRight(BiTuple<A, B> rightTuple) {
        return new BiTuple<>(rightTuple.factA, rightTuple.factB, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(UniTuple<A> leftTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = leftTuple.factA;
    }

    @Override
    protected void updateOutTupleFromRight(BiTuple<A, B> rightTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
    }

}
