package ai.timefold.solver.constraint.streams.bavet.uni;

import ai.timefold.solver.constraint.streams.bavet.common.AbstractConcatNode;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

public final class BavetUniQuadConcatNode<A, B, C, D>
        extends AbstractConcatNode<UniTuple<A>, QuadTuple<A, B, C, D>, QuadTuple<A, B, C, D>> {

    BavetUniQuadConcatNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle, int inputStoreIndexLeftOutTupleList,
            int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromLeft(UniTuple<A> leftTuple) {
        return new QuadTuple<>(leftTuple.factA, null, null, null, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple) {
        return new QuadTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, rightTuple.factD, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(UniTuple<A> leftTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = leftTuple.factA;
    }

    @Override
    protected void updateOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
        outTuple.factD = rightTuple.factD;
    }

}
