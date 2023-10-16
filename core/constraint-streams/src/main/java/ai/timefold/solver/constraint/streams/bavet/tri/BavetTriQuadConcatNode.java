package ai.timefold.solver.constraint.streams.bavet.tri;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TriTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;

public final class BavetTriQuadConcatNode<A, B, C, D>
        extends AbstractConcatNode<TriTuple<A, B, C>, QuadTuple<A, B, C, D>, QuadTuple<A, B, C, D>> {

    public BavetTriQuadConcatNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromLeft(TriTuple<A, B, C> leftTuple) {
        return new QuadTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, null, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple) {
        return new QuadTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, rightTuple.factD, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(TriTuple<A, B, C> leftTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
        outTuple.factC = leftTuple.factC;
    }

    @Override
    protected void updateOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
        outTuple.factD = rightTuple.factD;
    }

}
