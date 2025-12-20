package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class ConcatTriUniNode<A, B, C>
        extends AbstractConcatNode<TriTuple<A, B, C>, UniTuple<A>, TriTuple<A, B, C>> {

    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;

    public ConcatTriUniNode(Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunctionB = paddingFunctionB;
        this.paddingFunctionC = paddingFunctionC;
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(TriTuple<A, B, C> leftTuple) {
        return TriTuple.of(leftTuple.getA(), leftTuple.getB(), leftTuple.getC(), outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(UniTuple<A> rightTuple) {
        var factA = rightTuple.getA();
        return TriTuple.of(factA, paddingFunctionB.apply(factA), paddingFunctionC.apply(factA), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(TriTuple<A, B, C> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
        outTuple.setC(leftTuple.getC());
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(rightTuple.getA());
    }

}
