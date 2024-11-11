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
        A factA = tuple.factA;
        B factB = tuple.factB;
        return new UniTuple<>(
                mappingFunction.apply(factA, factB),
                outputStoreSize);
    }

    @Override
    protected void remap(BiTuple<A, B> inTuple, UniTuple<NewA> outTuple) {
        A factA = inTuple.factA;
        B factB = inTuple.factB;
        NewA newA = mappingFunction.apply(factA, factB);
        outTuple.factA = newA;
    }

}
