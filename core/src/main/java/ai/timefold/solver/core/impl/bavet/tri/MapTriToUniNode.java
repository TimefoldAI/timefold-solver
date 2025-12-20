package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class MapTriToUniNode<A, B, C, NewA> extends AbstractMapNode<TriTuple<A, B, C>, UniTuple<NewA>> {

    private final TriFunction<A, B, C, NewA> mappingFunction;

    public MapTriToUniNode(int mapStoreIndex, TriFunction<A, B, C, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected UniTuple<NewA> map(TriTuple<A, B, C> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        var factC = tuple.getC();
        return UniTuple.of(mappingFunction.apply(factA, factB, factC), outputStoreSize);
    }

    @Override
    protected void remap(TriTuple<A, B, C> inTuple, UniTuple<NewA> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        var factC = inTuple.getC();
        var newA = mappingFunction.apply(factA, factB, factC);
        outTuple.setA(newA);
    }

}
