package ai.timefold.solver.core.impl.bavet.uni;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class MapUniToQuadNode<A, NewA, NewB, NewC, NewD>
        extends AbstractMapNode<UniTuple<A>, QuadTuple<NewA, NewB, NewC, NewD>> {

    private final Function<A, NewA> mappingFunctionA;
    private final Function<A, NewB> mappingFunctionB;
    private final Function<A, NewC> mappingFunctionC;
    private final Function<A, NewD> mappingFunctionD;

    public MapUniToQuadNode(int mapStoreIndex, Function<A, NewA> mappingFunctionA, Function<A, NewB> mappingFunctionB,
            Function<A, NewC> mappingFunctionC, Function<A, NewD> mappingFunctionD,
            TupleLifecycle<QuadTuple<NewA, NewB, NewC, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
        this.mappingFunctionC = Objects.requireNonNull(mappingFunctionC);
        this.mappingFunctionD = Objects.requireNonNull(mappingFunctionD);
    }

    @Override
    protected QuadTuple<NewA, NewB, NewC, NewD> map(UniTuple<A> tuple) {
        var factA = tuple.getA();
        return QuadTuple.of(mappingFunctionA.apply(factA), mappingFunctionB.apply(factA), mappingFunctionC.apply(factA),
                mappingFunctionD.apply(factA), outputStoreSize);
    }

    @Override
    protected void remap(UniTuple<A> inTuple, QuadTuple<NewA, NewB, NewC, NewD> outTuple) {
        var factA = inTuple.getA();
        outTuple.setA(mappingFunctionA.apply(factA));
        outTuple.setB(mappingFunctionB.apply(factA));
        outTuple.setC(mappingFunctionC.apply(factA));
        outTuple.setD(mappingFunctionD.apply(factA));
    }

}
