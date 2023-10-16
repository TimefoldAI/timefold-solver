package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;
 final class BavetUniTriConcatNode<A, B, C>
        extends AbstractConcatNode<UniTuple<A>, TriTuple<A, B, C>, TriTuple<A, B, C>> {
     BavetUniTriConcatNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
                           int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(UniTuple<A> leftTuple) {
        return new TriTuple<>(leftTuple.factA, null, null, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return new TriTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(UniTuple<A> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = leftTuple.factA;
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
    }

}
