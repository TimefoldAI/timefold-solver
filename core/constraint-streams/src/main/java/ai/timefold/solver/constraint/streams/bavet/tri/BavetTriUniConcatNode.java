package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class BavetTriUniConcatNode<A, B, C>
        extends AbstractConcatNode<TriTuple<A, B, C>, UniTuple<A>, TriTuple<A, B, C>> {

    BavetTriUniConcatNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(TriTuple<A, B, C> leftTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(UniTuple<A> rightTuple) {
        return new TriTuple<>(rightTuple.factA, null, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(TriTuple<A, B, C> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
        outTuple.factC = leftTuple.factC;
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = rightTuple.factA;
    }

}
