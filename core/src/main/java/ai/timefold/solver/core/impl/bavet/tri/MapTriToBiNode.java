package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class MapTriToBiNode<A, B, C, NewA, NewB> extends AbstractMapNode<TriTuple<A, B, C>, BiTuple<NewA, NewB>> {

    private final TriFunction<A, B, C, NewA> mappingFunctionA;
    private final TriFunction<A, B, C, NewB> mappingFunctionB;

    public MapTriToBiNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunctionA,
            TriFunction<A, B, C, NewB> mappingFunctionB,
            TupleLifecycle<BiTuple<NewA, NewB>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunctionA = Objects.requireNonNull(mappingFunctionA);
        this.mappingFunctionB = Objects.requireNonNull(mappingFunctionB);
    }

    @Override
    protected BiTuple<NewA, NewB> map(TriTuple<A, B, C> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        return BiTuple.of(mappingFunctionA.apply(factA, factB, factC), mappingFunctionB.apply(factA, factB, factC),
                outputStoreSize);
    }

    @Override
    protected void remap(TriTuple<A, B, C> inTuple, BiTuple<NewA, NewB> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        outTuple.setA(mappingFunctionA.apply(factA, factB, factC));
        outTuple.setB(mappingFunctionB.apply(factA, factB, factC));
    }

}
