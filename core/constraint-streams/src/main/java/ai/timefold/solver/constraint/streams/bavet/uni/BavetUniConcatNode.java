package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class BavetUniConcatNode<A> extends AbstractConcatNode<UniTuple<A>> {

    BavetUniConcatNode(TupleLifecycle<UniTuple<A>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
    }

    @Override
    protected UniTuple<A> getOutTuple(UniTuple<A> inTuple) {
        return new UniTuple<>(inTuple.factA, outputStoreSize);
    }

    @Override
    protected void updateOutTuple(UniTuple<A> inTuple, UniTuple<A> outTuple) {
        outTuple.updateIfDifferent(inTuple.factA);
    }
}
