package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapQuadToQuadNode<A, B, C, D, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<QuadTuple<A, B, C, D>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;
    private final QuadFunction<A, B, C, D, NewC> mappingFunctionC;
    private final QuadFunction<A, B, C, D, NewD> mappingFunctionD;

    public MapQuadToQuadNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB, QuadFunction<A, B, C, D, NewC> mappingFunctionC,
            QuadFunction<A, B, C, D, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(QuadTuple<A, B, C, D> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        var factD = tuple.getD();
        return QuadTuple.of(mappingFunctionA.apply(factA, factB, factC, factD),
                mappingFunctionB.apply(factA, factB, factC, factD), mappingFunctionC.apply(factA, factB, factC, factD),
                mappingFunctionD.apply(factA, factB, factC, factD), outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        var factD = inTuple.getD();
        outTuple.setA(mappingFunctionA.apply(factA, factB, factC, factD));
        outTuple.setB(mappingFunctionB.apply(factA, factB, factC, factD));
        outTuple.setC(mappingFunctionC.apply(factA, factB, factC, factD));
        outTuple.setD(mappingFunctionD.apply(factA, factB, factC, factD));
    }

}
