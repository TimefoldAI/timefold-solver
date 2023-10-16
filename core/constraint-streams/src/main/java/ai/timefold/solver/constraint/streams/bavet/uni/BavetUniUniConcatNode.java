package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

final class BavetUniUniConcatNode<A>
        extends AbstractConcatNode<UniTuple<A>, UniTuple<A>, UniTuple<A>> {

    BavetUniUniConcatNode(TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle,
                          int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
    }

    @Override
    protected UniTuple<A> getOutTupleFromLeft(UniTuple<A> leftTuple) {
        return new UniTuple<>(leftTuple.factA, outputStoreSize);
    }

    @Override
    protected UniTuple<A> getOutTupleFromRight(UniTuple<A> rightTuple) {
        return new UniTuple<>(rightTuple.factA, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(UniTuple<A> leftTuple, UniTuple<A> outTuple) {
        outTuple.factA = leftTuple.factA;
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, UniTuple<A> outTuple) {
        outTuple.factA = rightTuple.factA;
    }

}
