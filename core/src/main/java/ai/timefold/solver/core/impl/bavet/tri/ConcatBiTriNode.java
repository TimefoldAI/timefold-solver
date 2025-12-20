package ai.timefold.solver.core.impl.bavet.tri;

import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class ConcatBiTriNode<A, B, C>
        extends AbstractConcatNode<BiTuple<A, B>, TriTuple<A, B, C>, TriTuple<A, B, C>> {

    private final BiFunction<A, B, C> paddingFunction;

    public ConcatBiTriNode(BiFunction<A, B, C> paddingFunction,
            TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunction = paddingFunction;
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        var factA = leftTuple.getA();
        var factB = leftTuple.getB();
        return TriTuple.of(factA, factB, paddingFunction.apply(factA, factB), outputStoreSize);
    }

    @Override
    protected TriTuple<A, B, C> getOutTupleFromRight(TriTuple<A, B, C> rightTuple) {
        return TriTuple.of(rightTuple.getA(), rightTuple.getB(), rightTuple.getC(), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
    }

    @Override
    protected void updateOutTupleFromRight(TriTuple<A, B, C> rightTuple, TriTuple<A, B, C> outTuple) {
        outTuple.setA(rightTuple.getA());
        outTuple.setB(rightTuple.getB());
        outTuple.setC(rightTuple.getC());
    }

}
