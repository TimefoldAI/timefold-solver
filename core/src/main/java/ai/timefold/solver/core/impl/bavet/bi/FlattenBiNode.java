package ai.timefold.solver.core.impl.bavet.bi;

import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenBiNode<A, B, NewC> extends AbstractFlattenNode<BiTuple<A, B>, TriTuple<A, B, NewC>, NewC> {

    private final BiFunction<A, B, Iterable<NewC>> mappingFunction;
    private final int outputStoreSize;

    public FlattenBiNode(int flattenLastStoreIndex, BiFunction<A, B, Iterable<NewC>> mappingFunction,
            TupleLifecycle<TriTuple<A, B, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, NewC> createTuple(BiTuple<A, B> originalTuple, NewC newC) {
        return TriTuple.of(originalTuple.getA(), originalTuple.getB(), newC, outputStoreSize);
    }

    @Override
    protected Iterable<NewC> extractIterable(BiTuple<A, B> tuple) {
        return mappingFunction.apply(tuple.getA(), tuple.getB());
    }

}
