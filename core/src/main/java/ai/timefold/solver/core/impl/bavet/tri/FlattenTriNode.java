package ai.timefold.solver.core.impl.bavet.tri;

import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenLastNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenTriNode<A, B, C, NewD>
        extends AbstractFlattenLastNode<TriTuple<A, B, C>, QuadTuple<A, B, C, NewD>, NewD> {

    private final TriFunction<A, B, C, Iterable<NewD>> mappingFunction;
    private final int outputStoreSize;

    public FlattenTriNode(int flattenLastStoreIndex, TriFunction<A, B, C, Iterable<NewD>> mappingFunction,
            TupleLifecycle<QuadTuple<A, B, C, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, nextNodesTupleLifecycle);
        this.mappingFunction = Objects.requireNonNull(mappingFunction);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, NewD> createTuple(TriTuple<A, B, C> originalTuple, NewD newD) {
        return QuadTuple.of(originalTuple.getA(), originalTuple.getB(), originalTuple.getC(), newD, outputStoreSize);
    }

    @Override
    protected Iterable<NewD> extractIterable(TriTuple<A, B, C> tuple) {
        return mappingFunction.apply(tuple.getA(), tuple.getB(), tuple.getC());
    }

}
