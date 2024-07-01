package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConcatTriUniNode<A, B, C>
        extends AbstractConcatNode<TriTuple<A, B, C>, UniTuple<A>, TriTuple<A, B, C>> {

    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;

    ConcatTriUniNode(Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(TriTuple<A, B, C> leftTuple) {
        return new TriTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(UniTuple<A> rightTuple) {
        var factA = rightTuple.factA;
        return new TriTuple<>(factA,
                paddingFunctionB.apply(factA), paddingFunctionC.apply(factA),
                outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(TriTuple<A, B, C> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
        outTuple.factC = leftTuple.factC;
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.factA = rightTuple.factA;
    }

}
