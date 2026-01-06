package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapBiToTriNode<A, B, NewA, NewB, NewC> extends AbstractMapNode<BiTuple<A, B>, TriTuple<NewA, NewB, NewC>> {

    private final BiFunction<A, B, NewA> mappingFunctionA;
    private final BiFunction<A, B, NewB> mappingFunctionB;
    private final BiFunction<A, B, NewC> mappingFunctionC;

    public MapBiToTriNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunctionA, BiFunction<A, B, NewB> mappingFunctionB,
            BiFunction<A, B, NewC> mappingFunctionC,
            TupleLifecycle<TriTuple<NewA, NewB, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
    }

    @Override
    protected TriTuple<NewA, NewB, NewC> map(BiTuple<A, B> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        return TriTuple.of(mappingFunctionA.apply(factA, factB), mappingFunctionB.apply(factA, factB),
                mappingFunctionC.apply(factA, factB), outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, TriTuple<NewA, NewB, NewC> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        outTuple.setA(mappingFunctionA.apply(factA, factB));
        outTuple.setB(mappingFunctionB.apply(factA, factB));
        outTuple.setC(mappingFunctionC.apply(factA, factB));
    }

}
