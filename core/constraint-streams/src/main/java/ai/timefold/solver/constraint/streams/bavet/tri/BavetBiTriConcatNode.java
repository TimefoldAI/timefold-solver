package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.BiTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

final class BavetBiTriConcatNode<A, B, C>
        extends AbstractConcatNode<BiTuple<A, B>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    BavetBiTriConcatNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, null, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return new TriTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
    }

}
