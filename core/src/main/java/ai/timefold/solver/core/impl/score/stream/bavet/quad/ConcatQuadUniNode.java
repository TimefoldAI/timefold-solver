package ai.timefold.solver.core.impl.score.stream.bavet.quad;

import java.util.function.Function;

import ai.timefold.solver.core.impl.score.stream.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;

final class ConcatQuadUniNode<A, B, C, D>
        extends AbstractConcatNode<QuadTuple<A, B, C, D>, UniTuple<A>, QuadTuple<A, B, C, D>> {

    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;
    private final Function<A, D> paddingFunctionD;

    ConcatQuadUniNode(Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC, Function<A, D> paddingFunctionD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromLeft(QuadTuple<A, B, C, D> leftTuple) {
        return new QuadTuple<>(leftTuple.factA, leftTuple.factB, leftTuple.factC, leftTuple.factD, outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromRight(UniTuple<A> rightTuple) {
        var factA = rightTuple.factA;
        return new QuadTuple<>(factA,
                paddingFunctionB.apply(factA), paddingFunctionC.apply(factA), paddingFunctionD.apply(factA),
                outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(QuadTuple<A, B, C, D> leftTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = leftTuple.factA;
        outTuple.factB = leftTuple.factB;
        outTuple.factC = leftTuple.factC;
        outTuple.factD = leftTuple.factD;
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.factA = rightTuple.factA;
    }

}
