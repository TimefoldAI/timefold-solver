package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractMapNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

public final class MapBiToUniNode<A, B, NewA> extends AbstractMapNode<BiTuple<A, B>, UniTuple<NewA>> {

    private final BiFunction<A, B, NewA> mappingFunction;

    public MapBiToUniNode(int mapStoreIndex, BiFunction<A, B, NewA> mappingFunction,
            TupleLifecycle<UniTuple<NewA>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(mapStoreIndex, nextNodesTupleLifecycle, outputStoreSize);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
    }

    @Override
    protected UniTuple<NewA> map(BiTuple<A, B> tuple) {
        var factA = tuple.getA();
        var factB = tuple.getB();
        return UniTuple.of(mappingFunction.apply(factA, factB), outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, UniTuple<NewA> outTuple) {
        var factA = inTuple.getA();
        var factB = inTuple.getB();
        outTuple.setA(mappingFunction.apply(factA, factB));
    }

}
