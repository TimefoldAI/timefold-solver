package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;

final class ConcatBiTriNode<A, B, C>
        extends AbstractConcatNode<BiTuple<A, B>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    private final BiFunction<A, B, C> paddingFunction;

    ConcatBiTriNode(BiFunction<A, B, C> paddingFunction,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunction = paddingFunction;
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        var factA = leftTuple.factA;
        var factB = leftTuple.factB;
        return new TriTuple<>(factA, factB, paddingFunction.apply(factA, factB), outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return new TriTuple<>(rightTuple.factA, rightTuple.factB, rightTuple.factC, outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = rightTuple.factA;
        outTuple.factB = rightTuple.factB;
        outTuple.factC = rightTuple.factC;
    }

}
