package ai.timefold.solver.core.impl.bavet.bi;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class ConcatBiUniNode<A, B>
        extends AbstractConcatNode<BiTuple<A, B>, UniTuple<A>, BiTuple<A, B>> {

    private final Function<A, B> paddingFunction;

    public ConcatBiUniNode(Function<A, B> paddingFunction, TupleLifecycle<BiTuple<A, B>> nextNodesTupleLifecycle,
            int inputStoreIndexLeftOutTupleList, int inputStoreIndexRightOutTupleList,
            int outputStoreSize) {
        super(nextNodesTupleLifecycle, inputStoreIndexLeftOutTupleList, inputStoreIndexRightOutTupleList, outputStoreSize);
        this.paddingFunction = paddingFunction;
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromLeft(BiTuple<A, B> leftTuple) {
        return BiTuple.of(leftTuple.getA(), leftTuple.getB(), outputStoreSize);
    }

    @Override
    protected BiTuple<A, B> getOutTupleFromRight(UniTuple<A> rightTuple) {
        var factA = rightTuple.getA();
        return BiTuple.of(factA, paddingFunction.apply(factA), outputStoreSize);
    }

    @Override
    protected void updateOutTupleFromLeft(BiTuple<A, B> leftTuple, BiTuple<A, B> outTuple) {
        outTuple.setA(leftTuple.getA());
        outTuple.setB(leftTuple.getB());
    }

    @Override
    protected void updateOutTupleFromRight(UniTuple<A> rightTuple, BiTuple<A, B> outTuple) {
        outTuple.setA(rightTuple.getA());
    }

}
