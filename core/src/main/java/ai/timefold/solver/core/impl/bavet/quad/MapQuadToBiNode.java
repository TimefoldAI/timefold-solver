package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapQuadToBiNode<A, B, C, D, NewA, NewB> extends AbstractMapNode<QuadTuple<A, B, C, D>, BiTuple<NewA, NewB>> {

    private final QuadFunction<A, B, C, D, NewA> mappingFunctionA;
    private final QuadFunction<A, B, C, D, NewB> mappingFunctionB;

    public MapQuadToBiNode(int mapStoreIndex, QuadFunction<A, B, C, D, NewA> mappingFunctionA,
            QuadFunction<A, B, C, D, NewB> mappingFunctionB, TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle,
            int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(QuadTuple<A, B, C, D> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        var factD = tuple.getD();
        return BiTuple.of(mappingFunctionA.apply(factA, factB, factC, factD),
                mappingFunctionB.apply(factA, factB, factC, factD), outputStoreSize);
    }

    @Override
    protected void remap(QuadTuple<A, B, C, D> inTuple, BiTuple<NewA, NewB> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        var factD = inTuple.getD();
        outTuple.setA(mappingFunctionA.apply(factA, factB, factC, factD));
        outTuple.setB(mappingFunctionB.apply(factA, factB, factC, factD));
    }

}
