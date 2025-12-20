package ai.timefold.solver.core.impl.bavet.quad;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class ConcatBiQuadNode<A, B, C, D>
        extends AbstractConcatNode<BiTuple<A, B>, QuadTuple<A, B, C, D>, QuadTuple<A, B, C, D>> {

    private final BiFunction<A, B, C> paddingFunctionC;
    private final BiFunction<A, B, D> paddingFunctionD;

    public ConcatBiQuadNode(BiFunction<A, B, C> paddingFunctionC, BiFunction<A, B, D> paddingFunctionD,
            TupleLifecycle<QuadTuple<A, B, C, D>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunctionC = paddingFunctionC;
        this.paddingFunctionD = paddingFunctionD;
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        var factA = leftTuple.getA();
        var factB = leftTuple.getB();
        return QuadTuple.of(factA, factB, paddingFunctionC.apply(factA, factB), paddingFunctionD.apply(factA, factB),
                outputStoreSize);
    }

    @Override
    protected QuadTuple<A, B, C, D> getOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple) {
        return QuadTuple.of(rightTuple.getA(), rightTuple.getB(), rightTuple.getC(), rightTuple.getD(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
    }

    @Override
    protected void updateOutTupleFromRight(QuadTuple<A, B, C, D> rightTuple, QuadTuple<A, B, C, D> outTuple) {
        outTuple.setA(rightTuple.getA());
        outTuple.setB(rightTuple.getB());
        outTuple.setC(rightTuple.getC());
        outTuple.setD(rightTuple.getD());
    }

}
