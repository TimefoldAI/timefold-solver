package ai.timefold.solver.core.impl.bavet.tri;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class ConcatTriTriNode<A, B, C>
        extends AbstractConcatNode<TriTuple<A, B, C>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    public ConcatTriTriNode(TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(TriTuple<A, B, C> leftTuple) {
        return TriTuple.of(leftTuple.getA(), leftTuple.getB(), leftTuple.getC(), outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return TriTuple.of(rightTuple.getA(), rightTuple.getB(), rightTuple.getC(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(TriTuple<A, B, C> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
        outTuple.setC(leftTuple.getC());
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(rightTuple.getA());
        outTuple.setB(rightTuple.getB());
        outTuple.setC(rightTuple.getC());
    }

}
