package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConcatBiUniNode<A, B>
        extends AbstractConcatNode<BiTuple<A, B>, UniTuple<A>, BiTuple<A, B>> {

    ConcatBiUniNode(TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return new BiTuple<>(leftTuple.factA, leftTuple.factB, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromRight(UniTuple<A> rightTuple) {
        return new BiTuple<>(rightTuple.factA, null, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, BiTuple<A, B> outTuple) {
        outTuple.factA = rightTuple.factA;
    }

}
