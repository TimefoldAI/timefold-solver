package ai.timefold.solver.core.impl.bavet.quad;

import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenLastQuadNode<A, B, C, D, NewD>
        extends AbstractFlattenNode<QuadTuple<A, B, C, D>, QuadTuple<A, B, C, NewD>, NewD> {

    private final Function<D, Iterable<NewD>> mappingFunction;
    private final int outputStoreSize;

    public FlattenLastQuadNode(int flattenLastStoreIndex, Function<D, Iterable<NewD>> mappingFunction,
            TupleLifecycle<QuadTuple<A, B, C, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, NewD> createTuple(QuadTuple<A, B, C, D> originalTuple, NewD newD) {
        return QuadTuple.of(originalTuple.getA(), originalTuple.getB(), originalTuple.getC(), newD, outputStoreSize);
    }

    @Override
    protected Iterable<NewD> extractIterable(QuadTuple<A, B, C, D> tuple) {
        return mappingFunction.apply(tuple.getD());
    }

}
