package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class ConcatBiQuadNode<A, B, C, D>
        extends AbstractConcatNode<BiTuple<A, B>, QuadTuple<A, B, C, D>, QuadTuple<A, B, C, D>> {

    ConcatBiQuadNode(TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return new QuadTuple<>(leftTuple.factA, leftTuple.factB, null, null, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple) {
        return new QuadTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, rightTuple.factD, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void updateOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
        outTuple.factD = rightTuple.factD;
    }

}
