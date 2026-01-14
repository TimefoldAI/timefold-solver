package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenLastTriNode<A, B, C, NewC>
        extends AbstractFlattenNode<TriTuple<A, B, C>, TriTuple<A, B, NewC>, NewC> {

    private final Function<C, Iterable<NewC>> mappingFunction;
    private final int outputStoreSize;

    public FlattenLastTriNode(int flattenLastStoreIndex, Function<C, Iterable<NewC>> mappingFunction,
            TupleLifecycle<TriTuple<A, B, NewC>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected TriTuple<A, B, NewC> createTuple(TriTuple<A, B, C> originalTuple, NewC newC) {
        return TriTuple.of(originalTuple.getA(), originalTuple.getB(), newC, outputStoreSize);
    }

    @Override
    protected Iterable<NewC> extractIterable(TriTuple<A, B, C> tuple) {
        return mappingFunction.apply(tuple.getC());
    }

}
