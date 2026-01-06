package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapQuadToTriNode<A, B, C, D, NewA, NewB, NewC>
        extends AbstractMapNode<QuadTuple<A, B, C, D>, TriTuple<NewA, NewB, NewC>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;
    private final QuadFunction<A, B, C, D, NewC> mappingFunctionC;

    public MapQuadToTriNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB, QuadFunction<A, B, C, D, NewC> mappingFunctionC,
            TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(QuadTuple<A, B, C, D> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        var factD = tuple.getD();
        return TriTuple.of(mappingFunctionA.apply(factA, factB, factC, factD),
                mappingFunctionB.apply(factA, factB, factC, factD), mappingFunctionC.apply(factA, factB, factC, factD),
                outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        var factD = inTuple.getD();
        outTuple.setA(mappingFunctionA.apply(factA, factB, factC, factD));
        outTuple.setB(mappingFunctionB.apply(factA, factB, factC, factD));
        outTuple.setC(mappingFunctionC.apply(factA, factB, factC, factD));
    }

}
