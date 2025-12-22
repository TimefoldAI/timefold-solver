package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapBiToQuadNode<A, B, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<BiTuple<A, B>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private final BiFunction<A, B, NewC> mappingFunctionC;
    private final BiFunction<A, B, NewD> mappingFunctionD;

    public MapBiToQuadNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunctionA, BiFunction<A, B, NewB> mappingFunctionB,
            BiFunction<A, B, NewC> mappingFunctionC, BiFunction<A, B, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(BiTuple<A, B> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        return QuadTuple.of(mappingFunctionA.apply(factA, factB), mappingFunctionB.apply(factA, factB),
                mappingFunctionC.apply(factA, factB), mappingFunctionD.apply(factA, factB), outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        outTuple.setA(mappingFunctionA.apply(factA, factB));
        outTuple.setB(mappingFunctionB.apply(factA, factB));
        outTuple.setC(mappingFunctionC.apply(factA, factB));
        outTuple.setD(mappingFunctionD.apply(factA, factB));
    }

}
