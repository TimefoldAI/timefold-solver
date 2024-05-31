package ai.timefold.solver.core.impl.score.stream.bavet.bi;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConcatBiUniNode<A, B>
        extends AbstractConcatNode<BiTuple<A, B>, UniTuple<A>, BiTuple<A, B>> {

    private final Function<A, B> paddingFunction;

    ConcatBiUniNode(Function<A, B> paddingFunction, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunction = paddingFunction;
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return new BiTuple<>(leftTuple.factA, leftTuple.factB, outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromRight(UniTuple<A> rightTuple) {
        var factA = rightTuple.factA;
        return new BiTuple<>(factA, paddingFunction.apply(factA), outputStoreSize);
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
