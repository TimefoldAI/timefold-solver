package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class ConcatUniTriNode<A, B, C>
        extends AbstractConcatNode<UniTuple<A>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    private final Function<A, B> paddingFunctionB;
    private final Function<A, C> paddingFunctionC;

    public ConcatUniTriNode(Function<A, B> paddingFunctionB, Function<A, C> paddingFunctionC,
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
        var factA = leftTuple.getA();
        return TriTuple.of(factA, paddingFunctionB.apply(factA), paddingFunctionC.apply(factA), outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return TriTuple.of(rightTuple.getA(), rightTuple.getB(), rightTuple.getC(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(UniTuple<A> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(leftTuple.getA());
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(rightTuple.getA());
        outTuple.setB(rightTuple.getB());
        outTuple.setC(rightTuple.getC());
    }

}
