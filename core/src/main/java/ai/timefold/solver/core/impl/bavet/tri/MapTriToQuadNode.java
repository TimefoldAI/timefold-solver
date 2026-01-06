package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapTriToQuadNode<A, B, C, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<TriTuple<A, B, C>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;
    private final TriFunction<A, B, C, NewC> mappingFunctionC;
    private final TriFunction<A, B, C, NewD> mappingFunctionD;

    public MapTriToQuadNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunctionA,
            TriFunction<A, B, C, NewB> mappingFunctionB, TriFunction<A, B, C, NewC> mappingFunctionC,
            TriFunction<A, B, C, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(TriTuple<A, B, C> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        return QuadTuple.of(mappingFunctionA.apply(factA, factB, factC), mappingFunctionB.apply(factA, factB, factC),
                mappingFunctionC.apply(factA, factB, factC), mappingFunctionD.apply(factA, factB, factC), outputStoreSize);
    }

    @Override
    protected void remap(TriTuple<A, B, C> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        outTuple.setA(mappingFunctionA.apply(factA, factB, factC));
        outTuple.setB(mappingFunctionB.apply(factA, factB, factC));
        outTuple.setC(mappingFunctionC.apply(factA, factB, factC));
        outTuple.setD(mappingFunctionD.apply(factA, factB, factC));
    }

}
