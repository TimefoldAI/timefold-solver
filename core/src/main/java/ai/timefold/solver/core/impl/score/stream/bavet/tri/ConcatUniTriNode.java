package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConcatUniTriNode<A, B, C>
        extends AbstractConcatNode<UniTuple<A>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;

    ConcatUniTriNode(Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList,
                outputStoreSize);
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(UniTuple<A> leftTuple) {
        var factA = leftTuple.factA;
        return new TriTuple<>(factA,
                paddingFunctionB.apply(factA), paddingFunctionC.apply(factA),
                outputStoreSize);
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
