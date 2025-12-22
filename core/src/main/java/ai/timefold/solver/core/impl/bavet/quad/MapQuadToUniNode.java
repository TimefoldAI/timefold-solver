package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class MapQuadToUniNode<A, B, C, D, NewA> extends AbstractMapNode<QuadTuple<A, B, C, D>, UniTuple<NewA>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunction;

    public MapQuadToUniNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected UniTuple<NewA> map(QuadTuple<A, B, C, D> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        var factD = tuple.getD();
        return UniTuple.of(mappingFunction.apply(factA, factB, factC, factD), outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, UniTuple<NewA> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        var factD = inTuple.getD();
        var newA = mappingFunction.apply(factA, factB, factC, factD);
        outTuple.setA(newA);
    }

}
